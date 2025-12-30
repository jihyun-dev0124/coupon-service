package io.github.jihyundev.coupon_service.domain.campaign;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Builder
@Table(name="tb_coupon_campaign",
        uniqueConstraints = @UniqueConstraint(name="uk_campaign_coupon_id", columnNames = {"coupon_id"}))
public class CouponCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="coupon_id", nullable=false, length=64)
    private String couponId;

    @Column(name="name", nullable=false, length=100)
    private String name;

    @Column(name="total_stock", nullable=false)
    private long totalStock;

    @Column(name="start_at", nullable=false)
    private Instant startAt;

    @Column(name="end_at", nullable=false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=64)
    private CampaignStatus status;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Version
    private Long version;

    public void open(Instant now) {
        if(now.isBefore(startAt) || now.isAfter(endAt)) {
            throw new IllegalStateException("Coupon campaign 기간이 아닙니다.");
        }
        this.status = CampaignStatus.OPEN;
        this.updatedAt = now;
    }

    public void close(Instant now) {
        this.status = CampaignStatus.CLOSED;
        this.updatedAt = now;
    }

    public void end(Instant now) {
        this.status = CampaignStatus.ENDED;
        this.updatedAt = now;
    }

    public void updateWindow(Instant startAt, Instant endAt, Instant now) {
        if(!endAt.isAfter(startAt)) throw new IllegalStateException("종료일은 시작일 이후여야 합니다.");
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = now;
    }
}
