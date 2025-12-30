package io.github.jihyundev.coupon_service.application;

import io.github.jihyundev.coupon_service.application.event.CampaignRedisSyncEvent;
import io.github.jihyundev.coupon_service.domain.campaign.CampaignStatus;
import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaign;
import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaignRepository;
import io.github.jihyundev.coupon_service.domain.coupon.Coupon;
import io.github.jihyundev.coupon_service.domain.coupon.CouponRepository;
import io.github.jihyundev.coupon_service.domain.coupon.CouponStatus;
import io.github.jihyundev.coupon_service.dto.AdminCampaignCreateRequest;
import io.github.jihyundev.coupon_service.dto.AdminCampaignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static io.github.jihyundev.coupon_service.application.event.CampaignRedisSyncEvent.*;

@Service
@RequiredArgsConstructor
public class CouponCampaignAdminService {
    private final CouponCampaignRepository couponCampaignRepository;
    private final CouponRepository couponRepository;
    private final StringRedisTemplate redis;
    private final CampaignScheduleRedisService campaignScheduleRedisService;
    private final ApplicationEventPublisher publisher;

    private static String openKey(String couponId) {return "coupon:campaign:open:"+couponId;}
    private static String stockKey(String couponId) {return "coupon:stock:"+couponId;}
    private static String issuedKey(String couponId) {return "coupon:issued:"+couponId;}

    //종료 후 24시간 키 유지(감사/cs/리플레이 디버깅)
    private static final Duration TTL_GRACE = Duration.ofHours(24);

    @Transactional
    public AdminCampaignResponse create(AdminCampaignCreateRequest req) {
        if (!req.endAt().isAfter(req.startAt())) {
            throw new IllegalArgumentException("종료일은 시작일보다 이후여야 합니다.");
        }

        Instant now = Instant.now();
        CouponCampaign campaign = CouponCampaign.builder()
                .couponId(req.couponId())
                .name(req.name())
                .totalStock(req.totalStock())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .status(CampaignStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CouponCampaign saved = couponCampaignRepository.save(campaign);
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.UPSERT_SCHEDULE, saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminCampaignResponse get(String couponId) {
        CouponCampaign c = couponCampaignRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("해당 쿠폰 캠페인 정보를 찾을 수 없습니다."));
        return toResponse(c);
    }

    /**
     * 수동 오픈
     * - DB status OPEN으로 변경
     * - Redis openKey = 1, stock 초기화 (issued set은 유지/재오픈 정책에 따라 결정)
     * - TTL은 endAt+grace로 통일
     * @param couponId
     */
    @Transactional
    public void open(String couponId) {
        Instant now = Instant.now();

        Coupon coupon = couponRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("해당 쿠폰 정보를 찾을 수 없습니다."));
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성 쿠폰은 오픈할 수 없습니다.");
        }

        CouponCampaign c = couponCampaignRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("해당 쿠폰 캠페인 정보를 찾을 수 없습니다."));
        if(now.isBefore(c.getCreatedAt()) || now.isAfter(c.getEndAt())) {
            throw new IllegalArgumentException("캠페인 기간이 아닙니다.");
        }

        // CAS: DRAFT -> OPEN
        int updated = couponCampaignRepository.updateStatusIfMatch(couponId, CampaignStatus.DRAFT, CampaignStatus.OPEN, now);
        if(updated == 0) {
            return;
        }

        //최신 캠페인 정보 다시 조회
        CouponCampaign opened = couponCampaignRepository.findByCouponId(couponId).orElseThrow();

        //커밋 후 Redis 오픈 키, 재고, issued TTL 세팅 + 스케줄(score)도 최신화
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.OPEN_KEYS, opened));
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.UPSERT_SCHEDULE, opened));
    }

    /**
     * 수동 종료(조기 종료 포함)
     * - Redis openKey 삭제 => 즉시 차단
     * - DB status CLOSED
     * - stock/issued는 TTL 유지 (운영상 감사 목적)
     * @param couponId
     */
    @Transactional
    public void close(String couponId) {
        Instant now = Instant.now();

        int updated = couponCampaignRepository.updateStatusIfMatch(couponId, CampaignStatus.OPEN, CampaignStatus.CLOSED, now);
        if(updated == 0) {
            couponCampaignRepository.updateStatusIfMatch(couponId, CampaignStatus.DRAFT, CampaignStatus.CLOSED, now);
        }

        CouponCampaign c = couponCampaignRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다."));

        //커밋 후 즉시 차단, 스케줄 제거
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.DELETE_OPEN_KEY, c));
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.REMOVE_SCHEDULE, c));
    }

    /**
     * 자동 종료(기간 endAt 경과시)
     * @param couponId
     */
    @Transactional
    public void markEnded(String couponId) {
        Instant now = Instant.now();
        int updated = couponCampaignRepository.updateStatusIfMatch(couponId, CampaignStatus.OPEN, CampaignStatus.ENDED, now);
        if (updated == 0) return;

        CouponCampaign c = couponCampaignRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다."));
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.DELETE_OPEN_KEY, c));
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.REMOVE_SCHEDULE, c));
    }

    @Transactional
    public void updateWindow(String couponId, Instant startAt, Instant endAt) {
        if(!endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("종료일은 시작일보다 이후여야 합니다.");
        }

        Instant now = Instant.now();
        CouponCampaign c = couponCampaignRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("쿠폰 캠페인 정보를 찾을 수 없습니다."));
        c.updateWindow(startAt, endAt, now);

        //커밋 후 스케줄(score) 반영
        publisher.publishEvent(new CampaignRedisSyncEvent(Type.UPSERT_SCHEDULE, c));
    }

    private Duration computeTtl(Instant now, Instant endAt) {
        Instant expireAt = endAt.plus(TTL_GRACE);
        Duration ttl = Duration.between(now, expireAt);
        //이미 지난 캠페인인데 open을 호출하면 open()에서 걸러지지만 방어적으로 최소 TTL 보장
        if (ttl.isNegative() || ttl.isZero()) {
            return Duration.ofMinutes(10);
        }
        return ttl;
    }

    private AdminCampaignResponse toResponse(CouponCampaign campaign) {
        return new AdminCampaignResponse(
                campaign.getCouponId(),
                campaign.getName(),
                campaign.getTotalStock(),
                campaign.getStartAt(),
                campaign.getEndAt(),
                campaign.getStatus()
        );
    }


}
