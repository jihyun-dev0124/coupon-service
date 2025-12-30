package io.github.jihyundev.coupon_service.domain.coupon;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name="tb_coupon")
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="coupon_id", nullable = false, length = 64)
    private String couponId;

    @Column(name="name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name="discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name="discount_value", nullable = false)
    private long discountValue;

    @Column(name="max_discount_amount")
    private Long maxDiscountMount;

    @Column(name="min_order_amount")
    private Long minOrderAmount;

    @Column(name="valid_days")
    private int validDays; //발급 후 사용 가능 기간.

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private CouponStatus status;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public void activate(Instant now) {
        this.status = CouponStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.status = CouponStatus.INACTIVE;
        this.updatedAt = now;
    }
}
