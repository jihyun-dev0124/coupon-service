package io.github.jihyundev.coupon_service.application;

import io.github.jihyundev.coupon_service.domain.campaign.CampaignStatus;
import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaign;
import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> zsetClaimScript;

    private final CouponCampaignRepository couponCampaignRepository;
    private final CouponCampaignAdminService couponCampaignAdminService;
    private final CampaignScheduleRedisService scheduleRedisService;

    private static final int CLAIM_LIMIT = 200;


    @Scheduled(fixedDelayString = "${app.campaign.scheduler.tick-ms:1000}")
    public void tick() {
        long nowMs = Instant.now().toEpochMilli();

        //1) due open
        List<String> openIds = claim(scheduleRedisService.openZsetKey(), nowMs, CLAIM_LIMIT);
        if(!openIds.isEmpty()) {
            handleOpen(openIds);
        }

        //2) due end
        List<String> endIds = claim(scheduleRedisService.endZsetKey(), nowMs, CLAIM_LIMIT);
        if(!endIds.isEmpty()) {
            handleEnd(endIds);
        }

    }

    @SuppressWarnings("unchecked")
    private List<String> claim(String zsetKey, long nowMs, int limit) {
        Object res = redis.execute(zsetClaimScript, List.of(zsetKey), String.valueOf(nowMs), String.valueOf(limit));
        if(res == null) return List.of();
        return (List<String>) res;
    }

    @Transactional
    protected void handleOpen(List<String> couponIds) {
        Instant now = Instant.now();

        //1.DB 배치 조회
        List<CouponCampaign> campaigns = couponCampaignRepository.findByCouponIdIn(couponIds);

        //2.결과를 couponId -> campaign 맵핑
        Map<String, CouponCampaign> map = campaigns.stream().collect(Collectors.toMap(CouponCampaign::getCouponId, Function.identity(), (a, b) -> a));

        //3.입력 couponIds 기준으로 처리
        for (String couponId : couponIds) {
            CouponCampaign c = map.get(couponId);
            if (c == null) {
                //DB에 없는데 Redis에 남아있는 엔트리 정리
                scheduleRedisService.removeSchedule(couponId);
                continue;
            }

            try{
                if (c.getStatus() != CampaignStatus.DRAFT) {
                    scheduleRedisService.removeSchedule(couponId);
                    continue;
                }

                if(now.isBefore(c.getStartAt()) || now.isAfter(c.getEndAt())) {
                    scheduleRedisService.upsertSchedule(c); //수정된 경우
                    continue;
                }

                couponCampaignAdminService.open(couponId);
                log.info("Auto-opened campaign. couponId: {}", couponId);
            } catch (Exception e) {
                log.warn("Auto-open campaign failed. couponId: {}", couponId, e);
                //실패 시 재스케줄하여 다음 tick/리컨실에서 회족
                scheduleRedisService.upsertSchedule(c);
            }
        }
    }

    @Transactional
    protected void handleEnd(List<String> couponIds) {
        Instant now = Instant.now();

        //1.DB 배치 조회
        List<CouponCampaign> campaigns = couponCampaignRepository.findByCouponIdIn(couponIds);
        //2.결과를 couponId -> campaign 맵핑
        Map<String, CouponCampaign> map = campaigns.stream().collect(Collectors.toMap(CouponCampaign::getCouponId, Function.identity(), (a, b) -> a));

        for(String couponId : couponIds) {
            CouponCampaign c = map.get(couponId);
            if(c == null){
                scheduleRedisService.removeSchedule(couponId);
                continue;
            }

            try {
                //OPEN 이면서 endAt <= now인 경우만 종료처리
                if(c.getStatus() != CampaignStatus.OPEN) {
                    //이미 닫혔거나 ended면 스케줄 제거
                    scheduleRedisService.removeSchedule(couponId);
                    continue;
                }

                if (now.isBefore(c.getEndAt())) {
                    // 윈도우가 변경된 경우: 재스케줄
                    scheduleRedisService.upsertSchedule(c);
                    continue;
                }

                couponCampaignAdminService.markEnded(couponId);
                log.info("Auto-closed campaign. couponId: {}", couponId);

                //종료 처리 후 스케줄 제거
                scheduleRedisService.removeSchedule(couponId);
            }catch (Exception e) {
                log.warn("Auto-close campaign failed. couponId: {}", couponId, e);
                scheduleRedisService.upsertSchedule(c);
            }
        }

    }

}
