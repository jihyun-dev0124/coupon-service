package io.github.jihyundev.coupon_service.dto;

import io.github.jihyundev.coupon_service.domain.campaign.CampaignStatus;

import java.time.Instant;

public record AdminCampaignResponse (String couponId, String name, long totalStock, Instant startAt, Instant endAt, CampaignStatus status) {
}
