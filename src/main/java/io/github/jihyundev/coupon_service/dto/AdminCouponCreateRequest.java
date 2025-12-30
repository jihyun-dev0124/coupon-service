package io.github.jihyundev.coupon_service.dto;

import io.github.jihyundev.coupon_service.domain.coupon.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCouponCreateRequest(
        @NotBlank String couponId, @NotBlank String name, @NotNull DiscountType discountType, @Min(1) long discountValue,
        Long maxDiscountAmount, Long minOrderAmount, Integer validDays) {
}
