package io.github.jihyundev.coupon_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jihyundev.coupon_service.domain.coupon.CouponIssue;
import io.github.jihyundev.coupon_service.domain.coupon.CouponIssueRepository;
import io.github.jihyundev.coupon_service.domain.outbox.OutboxEvent;
import io.github.jihyundev.coupon_service.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {
    private final CouponIssueRepository couponIssueRepository;

    @KafkaListener(topics = "coupon.issue.requested", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onMessage(CouponIssueRequested msg, Acknowledgment ack) {
        try {
            //최종 멱등 방어: DB 유니크 (uk_coupon_user)
            CouponIssue issue = CouponIssue.builder()
                    .couponId(msg.userId())
                    .userId(msg.userId())
                    .issuedAt(Instant.now())
                    .build();

            couponIssueRepository.save(issue);
            ack.acknowledge();
        } catch (DataIntegrityViolationException dup) {
            //이미 발급된 건 : 정상으로 간주(멱등)
            log.info("Duplicate issue ignored. couponId={}, userId={}, eventId={}", msg.couponId(), msg.userId(), msg.eventId());
            ack.acknowledge();
        } catch (Exception ex) {
            //재시도는 ErrorHandler가 담당
            log.error("Failed to issue coupon. couponId={}, userId={}, eventId={}", msg.couponId(), msg.userId(), msg.eventId(), ex);
            throw ex;
        }
    }

}
