package com.example.video.feign;

import com.example.video.dto.PaymentQrcodeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "video-maker-pay-service")
public interface PaymentFeignClient {

    @GetMapping("/api/internal/pay/qrcode/{orderId}")
    PaymentQrcodeResponse getPaymentQrcode(@PathVariable("orderId") Long orderId);
}
