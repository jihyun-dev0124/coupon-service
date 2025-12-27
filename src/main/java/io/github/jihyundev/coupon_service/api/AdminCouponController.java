package io.github.jihyundev.coupon_service.api;

import io.github.jihyundev.coupon_service.application.CouponAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/coupons")
public class AdminCouponController {
    private final CouponAdminService couponAdminService;

    @PostMapping("/{couponId}/stock")
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
    }

}
