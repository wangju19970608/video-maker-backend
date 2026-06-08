package com.example.video.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.example.video.service.WechatPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/wechat")
public class WechatPayController {

    private final WxMaService wxMaService;
    private final WechatPaymentService wechatPaymentService;

    public WechatPayController(WxMaService wxMaService, WechatPaymentService wechatPaymentService) {
        this.wxMaService = wxMaService;
        this.wechatPaymentService = wechatPaymentService;
    }

    /**
     * 小程序拿code换取openid
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam("code") String code) {
        try {
            WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(code);
            log.info("Wechat login success, openid: {}", session.getOpenid());
            return ResponseEntity.ok(Collections.singletonMap("openid", session.getOpenid()));
        } catch (Exception e) {
            log.error("Wechat login failed", e);
            throw new RuntimeException("Wechat login failed: " + e.getMessage());
        }
    }

    /**
     * 发起微信 JSAPI 支付
     */
    @PostMapping("/pay/{orderId}")
    public ResponseEntity<Object> createPayment(
            @PathVariable Long orderId, 
            @RequestParam(value = "openid", required = false) String openIdParam,
            @RequestBody(required = false) Map<String, Object> body) {
        
        try {
            String openId = openIdParam;
            if (openId == null && body != null && body.containsKey("openid")) {
                openId = String.valueOf(body.get("openid"));
            }
            
            if (openId == null || openId.isEmpty()) {
                throw new IllegalArgumentException("Missing required parameter: openid");
            }
            
            Object jsapiParams = wechatPaymentService.createJsapiOrder(orderId, openId);
            return ResponseEntity.ok(jsapiParams);
        } catch (Exception e) {
            log.error("Failed to create wechat order for {}", orderId, e);
            throw new RuntimeException("Wechat pay failed: " + e.getMessage());
        }
    }
}
