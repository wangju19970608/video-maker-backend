package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.TemplateUpsertRequest;
import com.example.video.admin.service.AdminTemplateService;
import com.example.video.dto.TemplateView;
import com.example.video.service.TemplateAnalysisService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/templates")
public class AdminTemplateController {

    private final AdminTemplateService templateService;
    private final TemplateAnalysisService analysisService;

    public AdminTemplateController(AdminTemplateService templateService,
                                  TemplateAnalysisService analysisService) {
        this.templateService = templateService;
        this.analysisService = analysisService;
    }

    /**
     * 上传模板文件（docx 或 mp4），保存到 template/ 或 template/video/ 目录，
     * 并自动更新模板的 previewUrl 字段。
     * 如果是视频文件，自动触发 AI 分析生成表单配置。
     */
    @PostMapping("/{templateId}/upload-file")
    public ApiResponse<Map<String, String>> uploadTemplateFile(
            @PathVariable Long templateId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ApiResponse.fail("文件名不能为空");
        }

        String lower = originalFilename.toLowerCase();
        boolean isVideo = lower.endsWith(".mp4");
        boolean isDocx = lower.endsWith(".docx");
        if (!isVideo && !isDocx) {
            return ApiResponse.fail("仅支持 .docx 或 .mp4 文件");
        }

        // 解析 projectRoot（与 VideoTaskService 逻辑一致）
        Path current = Paths.get(System.getProperty("user.dir"));
        Path projectRoot = current.getParent() != null ? current.getParent() : current;
        Path templateRoot = projectRoot.resolve("template");

        Path saveDir = isVideo ? templateRoot.resolve("video") : templateRoot;
        Files.createDirectories(saveDir);

        // 用 templateId 作为文件名，避免冲突
        String ext = isVideo ? ".mp4" : ".docx";
        String filename = "tpl-" + templateId + ext;
        Path savePath = saveDir.resolve(filename);
        file.transferTo(savePath.toFile());

        // 相对路径存入数据库
        String relativePath = isVideo
                ? "template/video/" + filename
                : "template/" + filename;

        // 更新模板的 previewUrl
        templateService.updatePreviewUrl(templateId, relativePath);

        // 如果是视频，自动触发 AI 分析
        if (isVideo) {
            try {
                analysisService.startAnalysis(templateId, savePath);
            } catch (Exception e) {
                // AI 分析失败不影响上传成功，管理员可以手动配置
                System.err.println("AI 分析启动失败: " + e.getMessage());
            }
        }

        Map<String, String> result = new HashMap<>();
        result.put("path", relativePath);
        result.put("message", isVideo ? "上传成功，AI 正在分析视频内容..." : "上传成功");
        return ApiResponse.success(isVideo ? "上传成功，AI 正在分析视频内容..." : "上传成功", result);
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

    @PutMapping("/{templateId}/status")
    public ApiResponse<TemplateView> updateStatus(@PathVariable Long templateId,
                                                  @RequestParam Boolean enabled) {
        return ApiResponse.success("updated", templateService.updateStatus(templateId, enabled));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Boolean> deleteTemplate(@PathVariable Long templateId) {
        templateService.deleteTemplate(templateId);
        return ApiResponse.success(true);
    }
}
