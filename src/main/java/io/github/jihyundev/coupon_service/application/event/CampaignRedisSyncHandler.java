package io.github.jihyundev.coupon_service.application.event;

import io.github.jihyundev.coupon_service.application.CampaignScheduleRedisService;
import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignRedisSyncHandler {
    private final CampaignScheduleRedisService scheduleRedisService;
    private final StringRedisTemplate redis;

    private static String openKey(String couponId) {return "coupon:campaign:open:"+couponId;}
    private static String stockKey(String couponId) {return "coupon:stock:"+couponId;}
    private static String issuedKey(String couponId) {return "coupon:issued:"+couponId;}

    private static final Duration TTL_GRACE = Duration.ofHours(24);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CampaignRedisSyncEvent event) {
        CouponCampaign c = event.campaign();
        String couponId = c.getCouponId();

        try {
            switch (event.type()) {
                case UPSERT_SCHEDULE -> scheduleRedisService.upsertSchedule(c);
                case REMOVE_SCHEDULE -> scheduleRedisService.removeSchedule(couponId);
                case OPEN_KEYS -> {
                    Duration ttl = computeTtl(Instant.now(), c.getEndAt());
                    redis.opsForValue().set(openKey(couponId), "1", ttl);
                    redis.opsForValue().set(stockKey(couponId), String.valueOf(c.getTotalStock()), ttl);
                    redis.delete(issuedKey(couponId)); //재오픈, 재배포 시 재발급 방지할거면 예외 처리 필요
                    redis.expire(issuedKey(couponId), ttl);
                }
                case DELETE_OPEN_KEY -> redis.delete(openKey(couponId));
            }
        } catch (Exception e) {
            //실패해도 리컨실(5분)로 복구하도록 설계
            log.error("Redis sync failed after commit. type={}, couponId=[}", event.type(), couponId, e);
        }
    }

    private Duration computeTtl(Instant now, Instant endAt) {
        Instant expireAt = endAt.plus(TTL_GRACE);
        Duration ttl = Duration.between(now, expireAt);
        if (ttl.isNegative() || ttl.isZero()) return Duration.ofMinutes(10);
        return ttl;
    }

}
