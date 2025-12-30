package io.github.jihyundev.coupon_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AdminCampaignCreateRequest (@NotBlank String couponId,
                                          @NotBlank String name,
                                          @Min(1) long totalStock,
                                          @NotNull Instant startAt,
                                          @NotNull Instant endAt) {

}
