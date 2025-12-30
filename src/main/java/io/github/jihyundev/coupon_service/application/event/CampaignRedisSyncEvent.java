package io.github.jihyundev.coupon_service.application.event;

import io.github.jihyundev.coupon_service.domain.campaign.CouponCampaign;


public record CampaignRedisSyncEvent (Type type, CouponCampaign campaign){
    public enum Type {
        UPSERT_SCHEDULE, //start/end 변경 또는 생성 시
        REMOVE_SCHEDULE, //close/ended 시
        OPEN_KEYS,       //open 시 openKey/stockKey/issuedKey TTL 세팅
        DELETE_OPEN_KEY  //close/ended 시 openKey 삭제
    }
}
