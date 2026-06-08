package com.example.video.controller;

import com.example.video.dto.PaymentQrcodeResponse;
import com.example.video.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/pay")
public class PaymentInternalController {

    private final PaymentService paymentService;

    public PaymentInternalController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/qrcode/{orderId}")
    public PaymentQrcodeResponse getPaymentQrcode(@PathVariable Long orderId) {
        return paymentService.getPaymentQrcode(orderId);
    }
}
