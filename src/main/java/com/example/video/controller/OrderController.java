package com.example.video.controller;

import com.example.video.dto.CreateOrderRequest;
import com.example.video.dto.OrderView;
import com.example.video.dto.PaymentQrcodeResponse;
import com.example.video.dto.TemplateView;
import com.example.video.model.SysUser;
import com.example.video.security.UserAuthInterceptor;
import com.example.video.service.OrderService;
import com.example.video.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final com.example.video.service.VideoTaskService videoTaskService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService,
                           com.example.video.service.VideoTaskService videoTaskService,
                           PaymentService paymentService) {
        this.orderService = orderService;
        this.videoTaskService = videoTaskService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<OrderView> listOrders(HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute(UserAuthInterceptor.USER_ATTR);
        List<OrderView> list = orderService.listOrders(user.getId());
        list.forEach(this::attachHistory);
        return list;
    }

    @PostMapping
    public ResponseEntity<OrderView> createOrder(@Validated @RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        SysUser user = (SysUser) httpRequest.getAttribute(UserAuthInterceptor.USER_ATTR);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, user.getId()));
    }

    @GetMapping("/{orderId}")
    public OrderView getOrder(@PathVariable Long orderId, HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute(UserAuthInterceptor.USER_ATTR);
        OrderView view = orderService.getOrder(orderId, user.getId());
        attachHistory(view);
        return view;
    }

    private void attachHistory(OrderView view) {
        if (view != null && view.getId() != null) {
            view.setHistoricalTasks(videoTaskService.listHistoricalTasks(view.getId()));
        }
    }

    /**
     * 获取支付宝付款二维码（支持沙箱和正式环境）
     */
    @GetMapping("/{orderId}/payment-qrcode")
    public PaymentQrcodeResponse getPaymentQrcode(@PathVariable Long orderId, HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute(UserAuthInterceptor.USER_ATTR);
        orderService.findEntityByIdAndCheckOwner(orderId, user.getId()); // check order ownership first
        return paymentService.getPaymentQrcode(orderId);
    }

    @PutMapping("/{orderId}/pay")
    public OrderView payOrder(@PathVariable Long orderId, HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute(UserAuthInterceptor.USER_ATTR);
        return orderService.payOrder(orderId, user.getId());
    }

    @PutMapping("/{orderId}/status")
    public OrderView updateStatus(@PathVariable Long orderId, @RequestParam String status, HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute(UserAuthInterceptor.USER_ATTR);
        return orderService.updateOrderStatus(orderId, status, null, null, user.getId());
    }

    @GetMapping("/{orderId}/jump")
    public TemplateView jumpOrder(@PathVariable Long orderId, HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute(UserAuthInterceptor.USER_ATTR);
        return orderService.jumpTemplate(orderId, user.getId());
    }

    @DeleteMapping("/{orderId}")
    public Map<String, Boolean> deleteOrder(@PathVariable Long orderId, HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute(UserAuthInterceptor.USER_ATTR);
        orderService.deleteOrder(orderId, user.getId());
        return Collections.singletonMap("success", true);
    }
}