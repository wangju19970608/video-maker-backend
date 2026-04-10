package com.example.video.controller;

import com.example.video.service.OrderService;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sun.rmi.runtime.Log;

@Slf4j
@RestController
@RequestMapping("/api/wechat")
public class WechatNotifyController {

    private final WxPayService wxPayService;
    private final OrderService orderService;

    public WechatNotifyController(WxPayService wxPayService, OrderService orderService) {
        this.wxPayService = wxPayService;
        this.orderService = orderService;
    }

    /**
     * 微信支付异步回调
     *
     * @param xmlData 微信发来的回调xml
     * @return 给微信系统返回的成功xml文档
     */
    @PostMapping("/notify")
    public String parseOrderNotifyResult(@RequestBody String xmlData) {
        log.info("-------------------已进入支付回调-----------------------");
        try {
            WxPayOrderNotifyResult result = wxPayService.parseOrderNotifyResult(xmlData);
            log.info("Receive Wechat Pay notification: tradeNo={}, result={}", result.getOutTradeNo(), result.getResultCode());

            if ("SUCCESS".equals(result.getResultCode())) {
                String outTradeNo = result.getOutTradeNo();
                // 收到支付成功通知，去数据库里将订单改为已付款
                orderService.payOrderByOrderNo(outTradeNo);
            }

            return WxPayNotifyResponse.success("成功");
        } catch (Exception e) {
            log.error("Wechat notify parse error", e);
            return WxPayNotifyResponse.fail("解析失败");
        }
    }
}
