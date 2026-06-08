package com.example.video.controller;

import com.example.video.model.WechatCustomerQrcode;
import com.example.video.service.WechatCustomerQrcodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/qrcode")
public class CustomerQrcodeController {

    private final WechatCustomerQrcodeService service;

    public CustomerQrcodeController(WechatCustomerQrcodeService service) {
        this.service = service;
    }

    @GetMapping
    public List<WechatCustomerQrcode> listEnabled() {
        return service.listEnabled();
    }
}