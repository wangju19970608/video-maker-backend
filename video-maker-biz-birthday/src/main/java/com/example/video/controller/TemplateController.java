package com.example.video.controller;

import com.example.video.dto.OptionView;
import com.example.video.dto.TemplateView;
import com.example.video.service.TemplateService;
import com.example.video.service.VideoTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final VideoTaskService videoTaskService;

    public TemplateController(TemplateService templateService, VideoTaskService videoTaskService) {
        this.templateService = templateService;
        this.videoTaskService = videoTaskService;
    }

    @GetMapping("/themes")
    public List<OptionView> listThemes() {
        return templateService.listThemes();
    }

    @GetMapping("/categories")
    public List<OptionView> listCategories() {
        return templateService.listCategories();
    }

    @GetMapping
    public List<TemplateView> listTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) String category) {
        return templateService.listTemplates(keyword, theme, category);
    }

    @GetMapping("/{templateId}")
    public TemplateView getTemplate(@PathVariable Long templateId) {
        return templateService.getTemplate(templateId);
    }

    @GetMapping("/internal/{templateId}/docx")
    public byte[] downloadTemplateDocx(@PathVariable Long templateId) throws Exception {
        com.example.video.model.VideoTemplate template = templateService.findEntityById(templateId);
        java.nio.file.Path docxPath = videoTaskService.resolveDocxPath(template);
        return java.nio.file.Files.readAllBytes(docxPath);
    }
}