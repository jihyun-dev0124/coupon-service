package io.github.jihyundev.coupon_service.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AdminCampaignUpdateWindowRequest(@NotNull Instant startAt, @NotNull Instant endAt) {
}
