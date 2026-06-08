package com.example.video.admin.service;

import com.example.video.dto.OrderView;
import com.example.video.model.OrderRecord;
import com.example.video.service.OrderService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    private final com.example.video.repository.OrderRecordRepository orderRepository;
    private final OrderService orderService;

    public AdminOrderService(com.example.video.repository.OrderRecordRepository orderRepository,
                             OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public List<OrderView> listOrders(String keyword, String status, String startDate, String endDate) {
        Specification<OrderRecord> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                Predicate orderNoLike = cb.like(cb.lower(root.get("orderNo")), likeKeyword);
                Predicate productLike = cb.like(cb.lower(root.get("productId")), likeKeyword);
                Predicate customerLike = cb.like(cb.lower(root.get("customerName")), likeKeyword);
                Predicate templateLike = cb.like(cb.lower(root.join("template").get("name")), likeKeyword);
                predicates.add(cb.or(orderNoLike, productLike, customerLike, templateLike));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim().toLowerCase()));
            }

            LocalDateTime startTime = parseStart(startDate);
            LocalDateTime endTime = parseEnd(endDate);
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return orderRepository.findAll(specification, sort).stream()
                .map(orderService::toView)
                .collect(Collectors.toList());
    }

    public OrderView getOrder(Long orderId) {
        return orderService.getOrder(orderId);
    }

    public OrderView updateOrderStatus(Long orderId, String status, java.math.BigDecimal amount, Integer maxGenerateCount) {
        return orderService.updateOrderStatus(orderId, status, amount, maxGenerateCount);
    }

    public void deleteOrder(Long orderId) {
        orderService.deleteOrder(orderId);
    }

    private LocalDateTime parseStart(String dateText) {
        if (!StringUtils.hasText(dateText)) {
            return null;
        }
        return LocalDate.parse(dateText).atStartOfDay();
    }

    private LocalDateTime parseEnd(String dateText) {
        if (!StringUtils.hasText(dateText)) {
            return null;
        }
        return LocalDate.parse(dateText).atTime(LocalTime.MAX);
    }
}