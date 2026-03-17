package com.example.video.controller;

import com.example.video.dto.CreateOrderRequest;
import com.example.video.dto.OrderView;
import com.example.video.dto.TemplateView;
import com.example.video.service.OrderService;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderView> listOrders() {
        return orderService.listOrders();
    }

    @PostMapping
    public ResponseEntity<OrderView> createOrder(@Validated @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    public OrderView getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @PutMapping("/{orderId}/pay")
    public OrderView payOrder(@PathVariable Long orderId) {
        return orderService.payOrder(orderId);
    }

    @PutMapping("/{orderId}/status")
    public OrderView updateStatus(@PathVariable Long orderId, @RequestParam String status) {
        return orderService.updateOrderStatus(orderId, status, null);
    }

    @GetMapping("/{orderId}/jump")
    public TemplateView jumpOrder(@PathVariable Long orderId) {
        return orderService.jumpTemplate(orderId);
    }

    @DeleteMapping("/{orderId}")
    public Map<String, Boolean> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return Collections.singletonMap("success", true);
    }
}