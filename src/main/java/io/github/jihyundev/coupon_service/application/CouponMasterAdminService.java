package io.github.jihyundev.coupon_service.application;

import io.github.jihyundev.coupon_service.domain.coupon.Coupon;
import io.github.jihyundev.coupon_service.domain.coupon.CouponRepository;
import io.github.jihyundev.coupon_service.domain.coupon.CouponStatus;
import io.github.jihyundev.coupon_service.dto.AdminCouponCreateRequest;
import io.github.jihyundev.coupon_service.dto.AdminCouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponMasterAdminService {
    private final CouponRepository couponRepository;

    @Transactional
    public AdminCouponResponse create(AdminCouponCreateRequest request) {
        Instant now = Instant.now();
        Coupon coupon = Coupon.builder()
                .couponId(request.couponId())
                .name(request.name())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .maxDiscountMount(request.maxDiscountAmount())
                .minOrderAmount(request.minOrderAmount())
                .validDays(request.validDays())
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Coupon saved = couponRepository.save(coupon);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminCouponResponse get(String couponId) {
        Coupon coupon = couponRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("쿠폰 정보를 찾을 수 없습니다."));
        return toResponse(coupon);
    }

    @Transactional
    public void deactivate(String couponId) {
        Coupon coupon = couponRepository.findByCouponId(couponId).orElseThrow(() -> new IllegalArgumentException("쿠폰 정보를 찾을 수 없습니다."));
        coupon.deactivate(Instant.now());
    }

    private AdminCouponResponse toResponse(Coupon c) {
        return new AdminCouponResponse(
                c.getCouponId(),
                c.getName(),
                c.getDiscountType(),
                c.getDiscountValue(),
                c.getMaxDiscountMount(),
                c.getMinOrderAmount(),
                c.getValidDays(),
                c.getStatus());
    }

}
