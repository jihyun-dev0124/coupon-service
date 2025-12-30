package io.github.jihyundev.coupon_service.api;

import io.github.jihyundev.coupon_service.application.CouponDownloadService;
import io.github.jihyundev.coupon_service.dto.DownloadResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponDownloadService couponDownloadService;

    @PostMapping("/{couponId}/download")
    public ResponseEntity<DownloadResponse> download(@PathVariable String couponId, @RequestParam @NotBlank String userId) {
        DownloadResponse response = couponDownloadService.requestDownload(couponId, userId);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }
}
