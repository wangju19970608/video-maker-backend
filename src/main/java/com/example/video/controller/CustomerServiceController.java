package com.example.video.controller;

import com.example.video.admin.model.AdminUser;
import com.example.video.admin.repository.AdminUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    private final AdminUserRepository adminUserRepository;

    public CustomerServiceController(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @GetMapping("/contact")
    public Map<String, Object> getContactInfo() {
        // 获取处于开启客服状态的管理员
        List<AdminUser> activeUsers = adminUserRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getCustomerServiceActive()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();

        if (!activeUsers.isEmpty()) {
            // 这里可以做轮询或者随机，这里简单取第一个配置了客服的人
            AdminUser user = activeUsers.get(0);
            response.put("nickname", user.getNickname());
            response.put("wechatQrUrl", user.getWechatQrUrl());
            response.put("wechatLink", user.getWechatLink());
            response.put("phone", user.getPhone());
        }

        return response;
    }
}
