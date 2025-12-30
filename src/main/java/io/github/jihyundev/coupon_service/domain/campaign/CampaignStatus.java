package io.github.jihyundev.coupon_service.domain.campaign;

public enum CampaignStatus {
    DRAFT, //생성만 됨(오픈 전)
    OPEN,
    CLOSED, //수동 종료(조기 종료 포함)
    ENDED   //기간 종료
}
