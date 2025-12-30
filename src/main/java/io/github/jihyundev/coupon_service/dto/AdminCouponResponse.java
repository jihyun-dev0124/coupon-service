package io.github.jihyundev.coupon_service.dto;

import io.github.jihyundev.coupon_service.domain.coupon.CouponStatus;
import io.github.jihyundev.coupon_service.domain.coupon.DiscountType;

public record AdminCouponResponse(
        String couponId,
        String name,
        DiscountType discountType,
        long discountValue,
        Long maxDiscountAmount,
        Long minOrderAmount,
        Integer validDays,
        CouponStatus status
) {
}
