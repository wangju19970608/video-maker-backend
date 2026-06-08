package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.TemplateConfigSaveRequest;
import com.example.video.admin.dto.TemplateConfigView;
import com.example.video.admin.service.AdminTemplateConfigService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/templates/{templateId}/config")
public class AdminTemplateConfigController {

    private final AdminTemplateConfigService configService;

    public AdminTemplateConfigController(AdminTemplateConfigService configService) {
        this.configService = configService;
    }

    /** 查询模板配置（含分析状态） */
    @GetMapping
    public ApiResponse<TemplateConfigView> getConfig(@PathVariable Long templateId) {
        return ApiResponse.success(configService.getConfig(templateId));
    }

    /** 上传视频并触发 AI 分析 */
    @PostMapping(value = "/upload-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TemplateConfigView> uploadVideo(@PathVariable Long templateId,
                                                       @RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择视频文件");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!originalName.endsWith(".mp4") && !originalName.endsWith(".mov") && !originalName.endsWith(".avi")) {
            throw new IllegalArgumentException("仅支持 mp4/mov/avi 格式");
        }
        return ApiResponse.success("上传成功，AI 分析已启动", configService.uploadAndAnalyse(templateId, file));
    }

    /** 管理员手动保存/调整 formFields 和 overlayRules */
    @PutMapping
    public ApiResponse<TemplateConfigView> saveConfig(@PathVariable Long templateId,
                                                      @RequestBody TemplateConfigSaveRequest request) throws Exception {
        return ApiResponse.success("配置已保存", configService.saveConfig(templateId, request));
    }
}
