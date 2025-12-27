package io.github.jihyundev.coupon_service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponAdminService {
    private final StringRedisTemplate redis;

    private static String stockKey(String couponId){
        return "coupon:stock:" + couponId;
    }
    private static String issuedSetKey(String couponId){
        return "coupon:issued:" + couponId;
    }

    /**
     * redis 쿠폰 재고 update
     * @param couponId
     * @param total
     */
    public void setStock(String couponId, long total) {
        redis.opsForValue().set(stockKey(couponId), String.valueOf(total));
    }

    /**
     * redis 쿠폰 정보 삭제
     * @param couponId
     */
    public void resetCoupon(String couponId) {
        redis.delete(stockKey(couponId));
        redis.delete(issuedSetKey(couponId));
    }

    /**
     * redis 쿠폰 남은 재고
     * @param couponId
     * @return
     */
    public long getRemainingCoupon(String couponId) {
        String v = redis.opsForValue().get(stockKey(couponId));
        if(v == null) return 0;
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            log.error("redis 남은 쿠폰 재고 조회 실패", e);
            return 0;
        }
    }

}
