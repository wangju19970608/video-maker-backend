package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.service.AdminTemplateService;
import com.example.video.model.WechatCustomerQrcode;
import com.example.video.service.WechatCustomerQrcodeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customer/qrcode")
public class AdminCustomerQrcodeController {

    private final WechatCustomerQrcodeService service;

    public AdminCustomerQrcodeController(WechatCustomerQrcodeService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WechatCustomerQrcode>> list() {
        return ApiResponse.success(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<WechatCustomerQrcode> get(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    public ApiResponse<WechatCustomerQrcode> create(@RequestBody WechatCustomerQrcode qrcode) {
        return ApiResponse.success("created", service.create(qrcode));
    }

    @PutMapping("/{id}")
    public ApiResponse<WechatCustomerQrcode> update(@PathVariable Long id, @RequestBody WechatCustomerQrcode qrcode) {
        return ApiResponse.success("updated", service.update(id, qrcode));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(true);
    }
}