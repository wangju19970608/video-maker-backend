package com.example.video.service;

import com.example.video.model.OrderRecord;
import com.example.video.repository.OrderRecordRepository;
import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class WechatPaymentService {

    private final WxPayService wxPayService;
    private final OrderRecordRepository orderRepository;

    @Value("${wechat.notify-url:https://your-domain.com/api/wechat/notify}")
    private String notifyUrl;

    public WechatPaymentService(WxPayService wxPayService, OrderRecordRepository orderRepository) {
        this.wxPayService = wxPayService;
        this.orderRepository = orderRepository;
    }

    public Object createJsapiOrder(Long orderId, String openId) throws Exception {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("未找到订单: " + orderId));

        if (wxPayService.getConfig().getMchId() == null || wxPayService.getConfig().getMchId().isEmpty()) {
            throw new RuntimeException("微信支付配置不完成，请在 application.properties 中配置 wechat.mch-id 和 wechat.mch-key");
        }

        WxPayUnifiedOrderRequest request = new WxPayUnifiedOrderRequest();
        request.setBody("视频制作-" + order.getOrderNo());
        request.setOutTradeNo(order.getOrderNo());
        // 微信支付单位为分
        request.setTotalFee(order.getAmount().multiply(new BigDecimal(100)).intValue()); 
        request.setSpbillCreateIp("127.0.0.1");
        request.setNotifyUrl(notifyUrl);
        request.setTradeType("JSAPI");
        request.setOpenid(openId);

        log.info("正在创建微信 JSAPI 订单: {}", request.getOutTradeNo());
        WxPayMpOrderResult result = wxPayService.createOrder(request);
        log.info("微信支付下单成功，订单号: {}", order.getOrderNo());
        return result;
    }

    /**
     * 关闭订单
     */
    public void closeOrder(String orderNo) {
        try {
            log.info("正在关闭微信订单: {}", orderNo);
            wxPayService.closeOrder(orderNo);
        } catch (Exception e) {
            log.error("关闭微信订单失败: {}", orderNo, e);
        }
    }
}
