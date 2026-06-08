package com.example.video.controller;

import com.example.video.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/api/pay/alipay")
@RequiredArgsConstructor
public class AlipayH5Controller {

    private final PaymentService paymentService;

    /**
     * 支付宝 H5 中转页：在浏览器打开此链接将直接唤起支付
     */
    @GetMapping(value = "/h5/{orderId}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String alipayH5Pay(@PathVariable Long orderId, HttpServletResponse response) {
        // 设置禁止缓存
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        
        // 返回包含自动提交表单的 HTML
        return paymentService.getAlipayWapForm(orderId);
    }
}
