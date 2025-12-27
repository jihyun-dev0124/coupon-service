package io.github.jihyundev.coupon_service.domain.outbox;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name="tb_outbox_event", uniqueConstraints = @UniqueConstraint(name="uk_outbox_event_id", columnNames = {"event_id"}),
        indexes = {@Index(name = "idx_outbox_status_id", columnList = "status,id"),
                   @Index(name = "idx_outbox_created_at", columnList = "created_at")})
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name="event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name="topic", nullable = false, length = 128)
    private String topic;

    @Column(name="aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Lob
    @Column(name="payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 16)
    private OutboxStatus status;

    @Column(name="fail_count", nullable = false)
    private int failCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    private Long version;

    public void markSent(Instant at) {
        this.status = OutboxStatus.SENT;
        this.publishedAt = at;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public void incFailCount() {
        this.failCount++;
    }




}
