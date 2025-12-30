package io.github.jihyundev.coupon_service.application;

import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaign;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CampaignScheduleRedisService {
    private final StringRedisTemplate redis;
    private static final String OPEN_ZSET = "zset:campaign:open";
    private static final String END_ZSET = "zset:campaign:end";

    public void upsertSchedule(CouponCampaign campaign) {
        String couponId = campaign.getCouponId();
        redis.opsForZSet().add(OPEN_ZSET, couponId, toScore(campaign.getStartAt()));
        redis.opsForZSet().add(END_ZSET, couponId, toScore(campaign.getEndAt()));
    }

    public void removeSchedule(String couponId) {
        redis.opsForZSet().remove(OPEN_ZSET, couponId);
        redis.opsForZSet().remove(END_ZSET, couponId);
    }

    public String openZsetKey(){return OPEN_ZSET;}
    public String endZsetKey(){return END_ZSET;}

    private double toScore(Instant t) {
        return (double) t.toEpochMilli();
    }
}
