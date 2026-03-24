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

    private final AdminOrderService adminOrderService;
    private final com.example.video.service.OrderService baseOrderService;
    private final com.example.video.service.VideoTaskService videoTaskService;

    public AdminOrderController(AdminOrderService adminOrderService,
                                com.example.video.service.OrderService baseOrderService,
                                com.example.video.service.VideoTaskService videoTaskService) {
        this.adminOrderService = adminOrderService;
        this.baseOrderService = baseOrderService;
        this.videoTaskService = videoTaskService;
    }

    @GetMapping
    public ApiResponse<List<OrderView>> listOrders(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        List<OrderView> list = adminOrderService.listOrders(keyword, status, startDate, endDate);
        list.forEach(this::attachHistory);
        return ApiResponse.success(list);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderView> getOrder(@PathVariable Long orderId) {
        OrderView view = adminOrderService.getOrder(orderId);
        attachHistory(view);
        return ApiResponse.success(view);
    }

    @PutMapping("/{orderId}")
    public ApiResponse<OrderView> updateOrder(@PathVariable Long orderId,
                                              @RequestBody OrderStatusRequest request) {
        OrderView view = adminOrderService.updateOrderStatus(orderId, request.getStatus(), request.getAmount(), request.getMaxGenerateCount());
        attachHistory(view);
        return ApiResponse.success("updated", view);
    }

    private void attachHistory(OrderView view) {
        if (view != null && view.getId() != null) {
            view.setHistoricalTasks(videoTaskService.listHistoricalTasks(view.getId()));
        }
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Boolean> deleteOrder(@PathVariable Long orderId) {
        adminOrderService.deleteOrder(orderId);
        return ApiResponse.success(true);
    }
    @GetMapping("/{orderId}/template-docx")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadTemplateDocx(@PathVariable Long orderId) throws Exception {
        com.example.video.model.OrderRecord order = baseOrderService.getOrderForTask(orderId);
        if (order == null || order.getTemplate() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        java.nio.file.Path docxPath = videoTaskService.resolveDocxPath(order.getTemplate());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(docxPath.toUri());
        
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"template_" + order.getTemplate().getId() + ".docx\"")
                .body(resource);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{orderId}/generate-custom")
    public ApiResponse<Object> generateCustom(@PathVariable Long orderId,
                                              @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        com.example.video.service.VideoTaskService.TaskRecord task = videoTaskService.createFromCustomDocx(orderId, file);
        return ApiResponse.success(task);
    }
}
