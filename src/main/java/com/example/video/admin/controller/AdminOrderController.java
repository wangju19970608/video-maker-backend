package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.OrderStatusRequest;
import com.example.video.admin.service.AdminOrderService;
import com.example.video.dto.OrderView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService orderService;

    public AdminOrderController(AdminOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderView>> listOrders(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        return ApiResponse.success(orderService.listOrders(keyword, status, startDate, endDate));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderView> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @PutMapping("/{orderId}")
    public ApiResponse<OrderView> updateOrder(@PathVariable Long orderId,
                                              @RequestBody OrderStatusRequest request) {
        return ApiResponse.success("updated", orderService.updateOrderStatus(orderId, request.getStatus(), request.getAmount()));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Boolean> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ApiResponse.success(true);
    }
}
