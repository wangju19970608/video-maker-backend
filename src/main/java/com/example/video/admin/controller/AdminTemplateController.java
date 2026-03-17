package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.TemplateUpsertRequest;
import com.example.video.admin.service.AdminTemplateService;
import com.example.video.dto.TemplateView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/templates")
public class AdminTemplateController {

    private final AdminTemplateService templateService;

    public AdminTemplateController(AdminTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResponse<List<TemplateView>> listTemplates(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String theme,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(templateService.listTemplates(keyword, theme, category, enabled));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateView> getTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(templateService.getTemplate(templateId));
    }

    @PostMapping
    public ApiResponse<TemplateView> createTemplate(@RequestBody TemplateUpsertRequest request) {
        return ApiResponse.success("created", templateService.createTemplate(request));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<TemplateView> updateTemplate(@PathVariable Long templateId,
                                                    @RequestBody TemplateUpsertRequest request) {
        return ApiResponse.success("updated", templateService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Boolean> deleteTemplate(@PathVariable Long templateId) {
        templateService.deleteTemplate(templateId);
        return ApiResponse.success(true);
    }
}
