package io.github.jihyundev.coupon_service.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {
    boolean existsByCouponIdAndUserId(String couponId, Long userId);
}
