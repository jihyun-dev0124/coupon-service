package io.github.jihyundev.coupon_service.dto;

import org.springframework.http.HttpStatus;

public record DownloadResponse (
        String result, //ACCEPTED, SOLD_OUT, ALREADY_ISSUED, FAILED
        String message,
        String requestId
){
    public static DownloadResponse accepted(String requestId) {
        return new DownloadResponse("ACCEPTED", "선착순 성공, 발급 처리 중입니다.", requestId);
    }

    public static DownloadResponse soldOut() {
        return new DownloadResponse("SOLD_OUT", "재고가 소진되었습니다.", null);
    }

    public static DownloadResponse alreadyIssued() {
        return new DownloadResponse("ALREADY_ISSUED", "이미 발급된 쿠폰입니다.", null);
    }

    public static DownloadResponse failed(String code, String msg) {
        return new DownloadResponse("FAILD", code + ": " + msg, null);
    }

    public int httpStatus() {
        return switch (result) {
            case "ACCEPTED" -> HttpStatus.ACCEPTED.value(); //202 (처리중)
            case "SOLD_OUT" -> HttpStatus.CONFLICT.value(); //409
            case "ALREADY_ISSUED" -> HttpStatus.OK.value();
            default -> HttpStatus.INTERNAL_SERVER_ERROR.value(); //500
        };
    }
}
