package io.github.jihyundev.coupon_service.domain.coupon;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name="coupon_issue",
        uniqueConstraints = @UniqueConstraint(name="uk_coupon_user", columnNames = {"coupon_id", "user_id"}),
        indexes = {
            @Index(name = "idx_coupon_id", columnList = "coupon_id"),
            @Index(name = "idx_user_id", columnList = "user_id")
        }
)
public class CouponIssue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="coupon_id",  nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name="issued_at", nullable = false)
    private Instant issuedAt;
}

