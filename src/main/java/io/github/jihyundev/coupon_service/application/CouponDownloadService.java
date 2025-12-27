package io.github.jihyundev.coupon_service.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jihyundev.coupon_service.domain.outbox.OutboxEvent;
import io.github.jihyundev.coupon_service.domain.outbox.OutboxEventRepository;
import io.github.jihyundev.coupon_service.domain.outbox.OutboxStatus;
import io.github.jihyundev.coupon_service.dto.DownloadResponse;
import io.github.jihyundev.coupon_service.messaging.CouponIssueRequested;
import io.github.jihyundev.coupon_service.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponDownloadService {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> couponGateScript;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonUtils json;

    private static final String EVENT_TYPE = "COUPON_ISSUE_REQUESTED";
    private static final String TOPIC = "coupon.issue.requested";

    private static String stockKey(String couponId) {
        return "coupon:stock:"+couponId;
    }
    private static String issuedSetKey(String couponId) {
        return "coupon:issued:"+couponId;
    }

    /**
     * API 트랜잭션 경계
     * 1) Redis Lua로 FCFS 게이트 통과 여부 결정(원자성)
     * 2) 통과 시 Outbox에 이벤트 적재(트랜잭션)
     * @param couponId
     * @param userId
     * @return
     */
    @Transactional
    public DownloadResponse requestDownload(String couponId, Long userId) {
        Long result;
        try {
            result = redis.execute(
                    couponGateScript,
                    List.of(stockKey(couponId), issuedSetKey(couponId)),
                    userId
            );
        } catch (DataAccessException e) {
            return DownloadResponse.failed("REDIS_ERROR", "요청 처리 중 오류가 발생했습니다.");
        }

        if (result == null) {
            return DownloadResponse.failed("REDIS_NULL", "요청 처리 중 오류가 발생했습니다.");
        }

        //0:sold out, 2:already
        if (result == 0L) return DownloadResponse.soldOut();
        if (result == 2L) return DownloadResponse.alreadyIssued();

        //1: ok -> outbox event insert
        CouponIssueRequested payload = CouponIssueRequested.builder()
                .eventId(UUID.randomUUID().toString())
                .couponId(couponId)
                .userId(userId)
                .build();

        OutboxEvent ev = OutboxEvent.builder()
                .eventId(payload.eventId())
                .eventType(EVENT_TYPE)
                .topic(TOPIC)
                .payload(json.toJson(payload))
                .status(OutboxStatus.NEW)
                .createdAt(Instant.now())
                .build();

        outboxEventRepository.save(ev);
        return DownloadResponse.accepted(payload.eventId());
    }

}
