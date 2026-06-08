package com.example.video.admin.service;

import com.example.video.admin.dto.DailySalesPointDto;
import com.example.video.admin.dto.OverviewStatsDto;
import com.example.video.admin.dto.TemplateSalesDto;
import com.example.video.admin.repository.AdminUserRepository;
import com.example.video.model.OrderRecord;
import com.example.video.repository.OrderRecordRepository;
import com.example.video.repository.VideoTemplateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PAID = "paid";

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderRecordRepository orderRepository;
    private final VideoTemplateRepository templateRepository;
    private final AdminUserRepository userRepository;

    public AdminStatsService(OrderRecordRepository orderRepository,
                             VideoTemplateRepository templateRepository,
                             AdminUserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
    }

    public OverviewStatsDto overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.atTime(LocalTime.MAX);

        OverviewStatsDto dto = new OverviewStatsDto();
        dto.setTodaySalesAmount(defaultAmount(orderRepository.sumAmountByStatusAndCreatedAtBetween(STATUS_PAID, startToday, endToday)));
        dto.setTodayPaidOrders(orderRepository.countByStatusAndCreatedAtBetween(STATUS_PAID, startToday, endToday));
        dto.setTodayOrders(orderRepository.countByCreatedAtBetween(startToday, endToday));

        dto.setTotalSalesAmount(defaultAmount(orderRepository.sumAmountByStatus(STATUS_PAID)));
        dto.setTotalPaidOrders(orderRepository.countByStatus(STATUS_PAID));
        dto.setTotalOrders(orderRepository.count());

        dto.setTotalUsers(userRepository.count());
        dto.setTotalTemplates(templateRepository.count());
        dto.setOnSaleTemplates(templateRepository.countByEnabledTrue());
        dto.setPendingOrders(orderRepository.countByStatus(STATUS_PENDING));

        return dto;
    }

    public List<DailySalesPointDto> dailySales(Integer days) {
        int queryDays = days == null ? 14 : Math.max(1, Math.min(days, 90));
        LocalDate endDay = LocalDate.now();
        LocalDate startDay = endDay.minusDays(queryDays - 1L);

        LocalDateTime startTime = startDay.atStartOfDay();
        LocalDateTime endTime = endDay.atTime(LocalTime.MAX);
        List<OrderRecord> records = orderRepository.findAllByCreatedAtBetween(startTime, endTime);

        Map<String, DailyAggregator> map = new HashMap<>();
        for (OrderRecord record : records) {
            String day = DAY_FORMATTER.format(record.getCreatedAt().toLocalDate());
            DailyAggregator aggregator = map.computeIfAbsent(day, key -> new DailyAggregator());
            aggregator.totalOrders++;

            if (STATUS_PAID.equalsIgnoreCase(record.getStatus())) {
                aggregator.paidOrders++;
                aggregator.salesAmount = aggregator.salesAmount.add(defaultAmount(record.getAmount()));
            }
        }

        List<DailySalesPointDto> result = new ArrayList<>();
        for (int i = 0; i < queryDays; i++) {
            LocalDate day = startDay.plusDays(i);
            String dayText = DAY_FORMATTER.format(day);
            DailyAggregator aggregator = map.getOrDefault(dayText, new DailyAggregator());

            DailySalesPointDto point = new DailySalesPointDto();
            point.setDay(dayText);
            point.setSalesAmount(aggregator.salesAmount);
            point.setPaidOrders(aggregator.paidOrders);
            point.setTotalOrders(aggregator.totalOrders);
            result.add(point);
        }
        return result;
    }

    public List<TemplateSalesDto> topTemplates(Integer limit) {
        int topN = limit == null ? 8 : Math.max(1, Math.min(limit, 50));
        List<OrderRecord> records = orderRepository.findAllByOrderByCreatedAtDesc();

        Map<Long, TemplateSalesDto> map = new HashMap<>();
        for (OrderRecord record : records) {
            if (!STATUS_PAID.equalsIgnoreCase(record.getStatus())) {
                continue;
            }
            Long templateId = record.getTemplate().getId();
            TemplateSalesDto dto = map.computeIfAbsent(templateId, key -> {
                TemplateSalesDto item = new TemplateSalesDto();
                item.setTemplateId(record.getTemplate().getId());
                item.setTemplateName(record.getTemplate().getName());
                item.setPaidOrders(0L);
                item.setSalesAmount(BigDecimal.ZERO);
                return item;
            });
            dto.setPaidOrders(dto.getPaidOrders() + 1);
            dto.setSalesAmount(dto.getSalesAmount().add(defaultAmount(record.getAmount())));
        }

        List<TemplateSalesDto> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparing(TemplateSalesDto::getSalesAmount).reversed());
        if (list.size() > topN) {
            return list.subList(0, topN);
        }
        return list;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static class DailyAggregator {
        private long totalOrders = 0;
        private long paidOrders = 0;
        private BigDecimal salesAmount = BigDecimal.ZERO;
    }
}