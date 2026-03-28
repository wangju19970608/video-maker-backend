package com.example.video.controller;

import com.example.video.service.OrderService;
import com.example.video.service.PaymentService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝异步回调接口
 * 支付宝在用户完成付款后，主动向 notify_url 发送 POST 请求通知支付结果
 * 该接口无需管理员鉴权（支付宝服务器调用）
 */
@RestController
@RequestMapping("/api/pay/alipay")
public class AlipayNotifyController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public AlipayNotifyController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    /**
     * 支付宝异步通知（POST，application/x-www-form-urlencoded）
     * 成功时返回 "success"，失败时返回 "fail"
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            // 收集所有参数
            Map<String, String> params = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String name = paramNames.nextElement();
                params.put(name, request.getParameter(name));
            }

            System.out.println("[支付宝回调] 收到通知参数: " + params);

            // 验签
            boolean verified = paymentService.verifyPayment(new HashMap<>(params));
            if (!verified) {
                System.err.println("[支付宝回调] 验签失败！");
                return "fail";
            }

            // 检查回调状态
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no"); // 我们的订单号

            if (!StringUtils.hasText(outTradeNo)) {
                return "fail";
            }

            // TRADE_SUCCESS 或 TRADE_FINISHED 均视为支付成功
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                System.out.println("[支付宝回调] 订单 " + outTradeNo + " 支付成功，更新状态...");
                // 根据 out_trade_no (orderNo) 找到订单并更新状态
                orderService.payOrderByOrderNo(outTradeNo);
                System.out.println("[支付宝回调] 订单 " + outTradeNo + " 状态更新为已支付");
            }

            return "success";
        } catch (Exception e) {
            System.err.println("[支付宝回调] 处理异常: " + e.getMessage());
            return "fail";
        }
    }
}
