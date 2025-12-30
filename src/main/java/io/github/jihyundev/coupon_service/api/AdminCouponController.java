package io.github.jihyundev.coupon_service.api;

import io.github.jihyundev.coupon_service.application.CouponAdminService;
import io.github.jihyundev.coupon_service.application.CouponCampaignAdminService;
import io.github.jihyundev.coupon_service.application.CouponMasterAdminService;
import io.github.jihyundev.coupon_service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/coupons")
public class AdminCouponController {
    private final CouponCampaignAdminService couponCampaignAdminService;
    private final CouponMasterAdminService couponMasterAdminService;

    // Coupon
    @PostMapping
    public ResponseEntity<AdminCouponResponse> createCoupon(@RequestBody @Valid AdminCouponCreateRequest request) {
        return ResponseEntity.ok(couponMasterAdminService.create(request));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<AdminCouponResponse> getCoupon(@PathVariable String couponId) {
        return ResponseEntity.ok(couponMasterAdminService.get(couponId));
    }

    @PostMapping("/{couponId}/deactivate")
    public ResponseEntity<Void> deactivateCoupon(@PathVariable String couponId) {
        couponMasterAdminService.deactivate(couponId);
        return ResponseEntity.ok().build();
    }


    // Campaign
    @PostMapping("/campaigns")
    public ResponseEntity<AdminCampaignResponse> createCampaign(@Valid @RequestBody AdminCampaignCreateRequest request) {
        return ResponseEntity.ok(couponCampaignAdminService.create(request));
    }

    @GetMapping("/campaigns/{couponId}")
    public ResponseEntity<AdminCampaignResponse> getCampaign(@PathVariable String couponId) {
        return ResponseEntity.ok(couponCampaignAdminService.get(couponId));
    }

    @PostMapping("/campaigns/{couponId}/open")
    public ResponseEntity<Void> openCampaign(@PathVariable String couponId) {
        couponCampaignAdminService.open(couponId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/campaigns/{couponId}/close")
    public ResponseEntity<Void> closeCampaign(@PathVariable String couponId) {
        couponCampaignAdminService.close(couponId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/campaigns/{couponId}/window")
    public ResponseEntity<Void> updateWindow(@PathVariable String couponId, @Valid @RequestBody AdminCampaignUpdateWindowRequest request) {
        couponCampaignAdminService.updateWindow(couponId, request.startAt(), request.endAt());
        return ResponseEntity.ok().build();
    }

    /*@PostMapping("/{couponId}/stock")
    public ResponseEntity<Void> setStock(@PathVariable String couponId, @RequestParam long total){
        couponAdminService.setStock(couponId, total);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> reset(@PathVariable String couponId){
        couponAdminService.resetCoupon(couponId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{couponId}/stock")
    public ResponseEntity<Long> getStock(@PathVariable String couponId){
        long stock = couponAdminService.getRemainingCoupon(couponId);
        return ResponseEntity.ok(stock);
    }*/
}
