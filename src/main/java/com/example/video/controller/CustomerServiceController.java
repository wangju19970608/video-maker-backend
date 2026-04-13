package com.example.video.controller;

import com.example.video.admin.model.AdminUser;
import com.example.video.admin.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceController.class);

    private final AdminUserRepository adminUserRepository;

    /** 企业微信 corpId，在 application.properties 中配置 wechat.corp-id */
    @Value("${wechat.corp-id:}")
    private String corpId;

    public CustomerServiceController(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    /**
     * 返回企微客服信息，供小程序调用 wx.openCustomerServiceChat 直接聊天。
     * corpId: 企业微信企业ID
     * kfUrl:  企微客服链接（在管理后台 wechatLink 字段中配置）
     */
    @GetMapping("/contact")
    public Map<String, Object> getContactInfo() {
        log.info("[客服接口] 开始查询客服配置，当前配置 corpId={}", corpId);

        List<AdminUser> allUsers = adminUserRepository.findAll();
        log.info("[客服接口] 数据库中总管理员数量={}", allUsers.size());
        for (AdminUser u : allUsers) {
            log.info("[客服接口] 管理员: id={}, nickname={}, customerServiceActive={}, wechatLink={}",
                    u.getId(), u.getNickname(), u.getCustomerServiceActive(), u.getWechatLink());
        }

        List<AdminUser> activeUsers = allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.getCustomerServiceActive()))
                .collect(Collectors.toList());
        log.info("[客服接口] 启用客服的管理员数量={}", activeUsers.size());

        Map<String, Object> response = new HashMap<>();

        if (!activeUsers.isEmpty()) {
            AdminUser user = activeUsers.get(0);
            String finalCorpId = (corpId != null && !corpId.isEmpty()) ? corpId : "";
            String kfUrl = user.getWechatLink();

            log.info("[客服接口] 使用客服账号: nickname={}, corpId={}, kfUrl={}",
                    user.getNickname(), finalCorpId, kfUrl);

            response.put("nickname", user.getNickname());
            response.put("corpId", finalCorpId);
            response.put("kfUrl", kfUrl);

            if (finalCorpId.isEmpty()) {
                log.warn("[客服接口] 警告: corpId 为空！请检查 application.properties 中的 wechat.corp-id 配置");
            }
            if (kfUrl == null || kfUrl.isEmpty()) {
                log.warn("[客服接口] 警告: kfUrl 为空！请在管理后台该账号的「企微链接」字段填写客服链接");
            }
        } else {
            log.warn("[客服接口] 没有找到任何启用客服的管理员，请在管理后台开启「客服开关」");
        }

        log.info("[客服接口] 返回数据: {}", response);
        return response;
    }
}
