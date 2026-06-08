package com.example.video.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "video-maker-order-service")
public interface OrderFeignClient {

    @PostMapping("/api/orders/internal/pay-by-no/{orderNo}")
    void payOrderByOrderNo(@PathVariable("orderNo") String orderNo);
}
