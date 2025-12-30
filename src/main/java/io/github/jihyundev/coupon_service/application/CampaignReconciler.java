package io.github.jihyundev.coupon_service.application;

import io.github.jihyundev.coupon_service.domain.campaign.CampaignStatus;
import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaign;
import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaignRepository;
import io.github.jihyundev.coupon_service.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignReconciler {

    private final CouponCampaignRepository campaignRepository;
    private final CampaignScheduleRedisService scheduleRedisService;

    /**
     * Redis 유실/배치 실패/운영 실수 대비:
     * - 가까운 미래(ex. 2일) 범위만 리컨실
     * - DRAFT(오픈 예정), OPEN(종료 예정)만 대상으로 스케줄 큐(ZSET)에 적재
     * 5~10분 간격
     */
    @Scheduled(fixedDelayString = "${app.campaign.reconcile.ms:300000}") //5분
    @Transactional(readOnly = true)
    public void reconcile() {
        Instant now = Instant.now();
        Instant horizon = now.plus(2, ChronoUnit.DAYS);

        // 1) DRAFT & startAt <= horizon & endAt >= now
        List<CouponCampaign> drafts = campaignRepository.findDraftInWindow(CampaignStatus.DRAFT, now, horizon);
        for (CouponCampaign c : drafts) {
            scheduleRedisService.upsertSchedule(c);
        }

        // 2) OPEN & endAt <= horizon
        List<CouponCampaign> opens = campaignRepository.findOpenEndingByHorizon(CampaignStatus.OPEN, horizon);
        for (CouponCampaign c : opens) {
            scheduleRedisService.upsertSchedule(c);
        }

        log.info("Campaign reconciling done. horizon={}, drafts={}, opens={}", horizon, drafts.size(), opens.size());
    }
}
