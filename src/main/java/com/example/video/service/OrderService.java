package com.example.video.service;

import com.example.video.dto.CreateOrderRequest;
import com.example.video.dto.OrderView;
import com.example.video.dto.TemplateView;
import com.example.video.exception.NotFoundException;
import com.example.video.model.OrderRecord;
import com.example.video.model.VideoTemplate;
import com.example.video.repository.OrderRecordRepository;
import com.example.video.repository.VideoTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PAID = "paid";
    public static final String STATUS_CANCELLED = "cancelled";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    private final OrderRecordRepository orderRepository;
    private final VideoTemplateRepository templateRepository;
    private final TemplateService templateService;

    public OrderService(OrderRecordRepository orderRepository,
                        VideoTemplateRepository templateRepository,
                        TemplateService templateService) {
        this.orderRepository = orderRepository;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
    }

    public List<OrderView> listOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    public OrderView getOrder(Long orderId) {
        return toView(findEntityById(orderId));
    }

    @Transactional
    public OrderView createOrder(CreateOrderRequest request) {
        VideoTemplate template = templateService.findEntityById(request.getTemplateId());

        OrderRecord record = new OrderRecord();
        record.setOrderNo(generateOrderNo());
        record.setProductId(buildProductId(template));
        record.setStatus(STATUS_PENDING);
        record.setAmount(template.getPrice() == null ? BigDecimal.ZERO : template.getPrice());
        record.setCustomerName(request.getCustomerName());
        record.setCustomerPhone(request.getCustomerPhone());
        record.setRemark(request.getRemark());
        record.setTemplate(template);

        return toView(orderRepository.save(record));
    }

    @Transactional
    public OrderView payOrder(Long orderId) {
        OrderRecord record = findEntityById(orderId);
        if (STATUS_PAID.equalsIgnoreCase(record.getStatus())) {
            return toView(record);
        }

        String previousStatus = normalizeStatus(record.getStatus());
        record.setStatus(STATUS_PAID);
        record.setPaidAt(LocalDateTime.now());
        adjustTemplateSales(record.getTemplate(), previousStatus, STATUS_PAID);

        return toView(orderRepository.save(record));
    }

    @Transactional
    public void payOrderByOrderNo(String orderNo) {
        orderRepository.findByOrderNo(orderNo).ifPresent(record -> {
            if (!STATUS_PAID.equalsIgnoreCase(record.getStatus())) {
                String previousStatus = normalizeStatus(record.getStatus());
                record.setStatus(STATUS_PAID);
                record.setPaidAt(LocalDateTime.now());
                adjustTemplateSales(record.getTemplate(), previousStatus, STATUS_PAID);
                orderRepository.save(record);
            }
        });
    }


    @Transactional
    public OrderView updateOrderStatus(Long orderId, String status, BigDecimal amount, Integer maxGenerateCount) {
        OrderRecord record = findEntityById(orderId);
        if (amount != null) {
            record.setAmount(amount);
        }
        if (maxGenerateCount != null) {
            record.setMaxGenerateCount(maxGenerateCount);
        }
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("status cannot be empty");
        }
        String normalized = status.trim().toLowerCase();
        if (!STATUS_PENDING.equals(normalized) && !STATUS_PAID.equals(normalized) && !STATUS_CANCELLED.equals(normalized)) {
            throw new IllegalArgumentException("unsupported order status: " + status);
        }

        String previousStatus = normalizeStatus(record.getStatus());
        if (previousStatus.equals(normalized) && amount == null) {
            return toView(record);
        }

        record.setStatus(normalized);
        if (STATUS_PAID.equals(normalized) && record.getPaidAt() == null) {
            record.setPaidAt(LocalDateTime.now());
        } else if (!STATUS_PAID.equals(normalized)) {
            record.setPaidAt(null);
        }
        adjustTemplateSales(record.getTemplate(), previousStatus, normalized);

        return toView(orderRepository.save(record));
    }

    @Transactional
    public void updateOrderTask(Long orderId, String taskId) {
        OrderRecord record = findEntityById(orderId);
        record.setTaskId(taskId);
        orderRepository.save(record);
    }
    
    @Transactional
    public OrderRecord getOrderForTask(Long orderId) {
        return findEntityById(orderId);
    }
    
    @Transactional
    public void incrementOrderTaskCount(Long orderId) {
        OrderRecord record = findEntityById(orderId);
        int current = record.getUsedGenerateCount() == null ? 0 : record.getUsedGenerateCount();
        record.setUsedGenerateCount(current + 1);
        orderRepository.save(record);
    }

    public TemplateView jumpTemplate(Long orderId) {
        OrderRecord record = findEntityById(orderId);
        if (!STATUS_PAID.equalsIgnoreCase(record.getStatus())) {
            throw new IllegalArgumentException("Order is not paid yet");
        }
        return templateService.toView(record.getTemplate());
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        OrderRecord record = findEntityById(orderId);
        if (STATUS_PAID.equalsIgnoreCase(record.getStatus())) {
            VideoTemplate template = record.getTemplate();
            long current = template.getSalesCount() == null ? 0L : template.getSalesCount();
            if (current > 0) {
                template.setSalesCount(current - 1);
                templateRepository.save(template);
            }
        }
        orderRepository.delete(record);
    }

    public OrderRecord findEntityById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    public OrderView toView(OrderRecord record) {
        OrderView view = new OrderView();
        view.setId(record.getId());
        view.setCreatedAt(formatTime(record.getCreatedAt()));
        view.setProductId(record.getProductId());
        view.setOrderNo(record.getOrderNo());
        view.setStatus(record.getStatus());
        view.setAmount(record.getAmount());
        view.setCustomerName(record.getCustomerName());
        view.setCustomerPhone(record.getCustomerPhone());
        view.setRemark(record.getRemark());
        view.setPaidAt(formatTime(record.getPaidAt()));
        view.setTaskId(record.getTaskId());
        view.setMaxGenerateCount(record.getMaxGenerateCount());
        view.setUsedGenerateCount(record.getUsedGenerateCount());
        view.setTemplate(templateService.toView(record.getTemplate()));
        return view;
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return TIME_FORMATTER.format(time);
    }

    private String generateOrderNo() {
        long timestamp = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return String.valueOf(timestamp) + suffix;
    }

    private String buildProductId(VideoTemplate template) {
        String digits = template.getTemplateCode() == null ? "" : template.getTemplateCode().replaceAll("\\D+", "");
        if (!StringUtils.hasText(digits)) {
            digits = String.valueOf(template.getId());
        }
        return "PRODUCT-ID:" + digits;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private void adjustTemplateSales(VideoTemplate template, String previousStatus, String nextStatus) {
        boolean wasPaid = STATUS_PAID.equals(previousStatus);
        boolean nowPaid = STATUS_PAID.equals(nextStatus);
        if (wasPaid == nowPaid) {
            return;
        }

        long current = template.getSalesCount() == null ? 0L : template.getSalesCount();
        if (nowPaid) {
            current += 1;
        } else {
            current = Math.max(0L, current - 1);
        }
        template.setSalesCount(current);
        templateRepository.save(template);
    }
}
