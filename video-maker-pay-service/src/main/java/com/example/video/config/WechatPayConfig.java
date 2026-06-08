package com.example.video.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class WechatPayConfig {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Value("${wechat.mch-id}")
    private String mchId;

    @Value("${wechat.mch-key}")
    private String mchKey;

    @Value("${wechat.notify-url}")
    private String notifyUrl;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(this.appId);
        config.setSecret(this.appSecret);

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }

    @Bean
    public WxPayService wxPayService() {
        if (this.mchId == null || this.mchId.trim().isEmpty() || "填入你的32位API密钥".equals(this.mchKey)) {
            log.warn("微信支付商户配置不完整 (mchId 或 mchKey 缺失)，WxPayService 将无法正常发起请求。");
        }

        WxPayConfig payConfig = new WxPayConfig();
        payConfig.setAppId(this.appId);
        payConfig.setMchId(this.mchId);
        payConfig.setMchKey(this.mchKey);
        payConfig.setNotifyUrl(this.notifyUrl);
        
        // 默认配置
        payConfig.setTradeType("JSAPI");
        payConfig.setSignType("MD5"); // 或者 HMAC-SHA256，取决于商户平台设置

        WxPayService wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(payConfig);
        return wxPayService;
    }
}
