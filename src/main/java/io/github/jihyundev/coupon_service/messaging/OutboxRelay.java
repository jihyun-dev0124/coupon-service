package io.github.jihyundev.coupon_service.messaging;

import io.github.jihyundev.coupon_service.domain.outbox.OutboxEvent;
import io.github.jihyundev.coupon_service.domain.outbox.OutboxEventRepository;
import io.github.jihyundev.coupon_service.domain.outbox.OutboxStatus;
import io.github.jihyundev.coupon_service.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, CouponIssueRequested> kafkaTemplate;
    private final JsonUtils json;

    private static final int BATCH_SIZE = 200;
    private static final int MAX_FAIL = 10;

    /**
     * 단순 DB PESSIMISTIC_WRITE + status 기반
     * - 단일 인스턴스만 도는 보장(락)까지 하려면 ShedLock 등 도입 권장
     */
    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository.findForUpdatedByStatus(OutboxStatus.NEW, PageRequest.of(0, BATCH_SIZE));
        if(batch.isEmpty()) return;

        for (OutboxEvent e : batch) {
            try {
                CouponIssueRequested payload = json.fromJson(e.getPayload(), CouponIssueRequested.class);

                //key는 couponId로 : 같은 쿠폰 이벤트를 파티션 단위로 어느정도 순서 보장
                kafkaTemplate.send(e.getTopic(), payload.couponId(), payload).get();
                e.markSent(Instant.now());
            } catch (Exception ex) {
                e.incFailCount();
                if(e.getFailCount() >= MAX_FAIL) {
                    e.markFailed();
                    log.error("Outbox relay permanently failed. eventId={}, id={}. failCount={}", e.getEventId(), e.getId(), e.getFailCount(), ex);
                }else{
                    log.warn("Outbox relay failed. eventId={}, id={}. failCount={}", e.getEventId(), e.getId(), e.getFailCount(), ex);
                }
            }
        }
    }

}
