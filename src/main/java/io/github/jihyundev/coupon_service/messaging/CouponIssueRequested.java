package io.github.jihyundev.coupon_service.messaging;

import lombok.Builder;

import java.time.Instant;

//Kafka 메시지 모델
@Builder
public record CouponIssueRequested (
        String eventId,
        String couponId,
        String userId,
        Instant requestedAt
){}
