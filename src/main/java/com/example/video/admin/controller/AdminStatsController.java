package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.DailySalesPointDto;
import com.example.video.admin.dto.OverviewStatsDto;
import com.example.video.admin.dto.TemplateSalesDto;
import com.example.video.admin.service.AdminStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatsController {

    private final AdminStatsService statsService;

    public AdminStatsController(AdminStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewStatsDto> overview() {
        return ApiResponse.success(statsService.overview());
    }

    @GetMapping("/daily-sales")
    public ApiResponse<List<DailySalesPointDto>> dailySales(@RequestParam(required = false) Integer days) {
        return ApiResponse.success(statsService.dailySales(days));
    }

    @GetMapping("/top-templates")
    public ApiResponse<List<TemplateSalesDto>> topTemplates(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(statsService.topTemplates(limit));
    }
}
