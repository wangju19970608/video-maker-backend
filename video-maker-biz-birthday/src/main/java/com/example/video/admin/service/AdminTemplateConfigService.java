package com.example.video.admin.service;

import com.example.video.admin.dto.TemplateConfigSaveRequest;
import com.example.video.admin.dto.TemplateConfigView;
import com.example.video.exception.NotFoundException;
import com.example.video.model.TemplateConfig;
import com.example.video.model.VideoTemplate;
import com.example.video.repository.TemplateConfigRepository;
import com.example.video.repository.VideoTemplateRepository;
import com.example.video.service.TemplateAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class AdminTemplateConfigService {

    private final VideoTemplateRepository templateRepository;
    private final TemplateConfigRepository configRepository;
    private final TemplateAnalysisService analysisService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminTemplateConfigService(VideoTemplateRepository templateRepository,
                                      TemplateConfigRepository configRepository,
                                      TemplateAnalysisService analysisService) {
        this.templateRepository = templateRepository;
        this.configRepository = configRepository;
        this.analysisService = analysisService;
    }

    /**
     * 上传模板视频文件，保存到 template/video/ 目录，更新 coverUrl，并触发 AI 分析。
     */
    @Transactional
    public TemplateConfigView uploadAndAnalyse(Long templateId, MultipartFile file) throws IOException {
        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        // 保存视频文件
        Path videoDir = resolveProjectRoot().resolve("template").resolve("video");
        Files.createDirectories(videoDir);

        String filename = templateId + ".mp4";
        Path videoPath = videoDir.resolve(filename);
        file.transferTo(videoPath.toFile());

        // 更新 coverUrl
        String coverUrl = "/api/assets/templates/video/" + filename;
        template.setCoverUrl(coverUrl);
        template.setTemplateType("video");
        templateRepository.save(template);

        // 触发 AI 分析（异步）
        TemplateConfig config = analysisService.startAnalysis(templateId, videoPath);
        return toView(config);
    }

    /**
     * 查询模板配置（含分析状态）。
     */
    public TemplateConfigView getConfig(Long templateId) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        TemplateConfig config = configRepository.findByTemplate_Id(templateId)
                .orElseGet(() -> {
                    TemplateConfig c = new TemplateConfig();
                    c.setAnalysisStatus("pending");
                    return c;
                });

        return toView(config);
    }

    /**
     * 管理员手动保存/覆盖 formFields 和 overlayRules。
     */
    @Transactional
    public TemplateConfigView saveConfig(Long templateId, TemplateConfigSaveRequest request) throws IOException {
        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        // 验证 JSON 合法性
        if (StringUtils.hasText(request.getFormFields())) {
            try { mapper.readTree(request.getFormFields()); }
            catch (Exception e) { throw new IllegalArgumentException("formFields 不是合法 JSON"); }
        }
        if (StringUtils.hasText(request.getOverlayRules())) {
            try { mapper.readTree(request.getOverlayRules()); }
            catch (Exception e) { throw new IllegalArgumentException("overlayRules 不是合法 JSON"); }
        }

        TemplateConfig config = configRepository.findByTemplate_Id(templateId)
                .orElseGet(() -> {
                    TemplateConfig c = new TemplateConfig();
                    c.setTemplate(template);
                    return c;
                });

        if (StringUtils.hasText(request.getFormFields())) {
            config.setFormFields(request.getFormFields());
        }
        if (StringUtils.hasText(request.getOverlayRules())) {
            config.setOverlayRules(request.getOverlayRules());
        }
        config.setAnalysisStatus("done");
        config.setAnalysisError(null);

        return toView(configRepository.save(config));
    }

    private TemplateConfigView toView(TemplateConfig config) {
        TemplateConfigView view = new TemplateConfigView();
        if (config.getTemplate() != null) {
            view.setTemplateId(config.getTemplate().getId());
        }
        view.setAnalysisStatus(config.getAnalysisStatus());
        view.setAnalysisError(config.getAnalysisError());

        view.setFormFields(parseJsonSafe(config.getFormFields()));
        view.setOverlayRules(parseJsonSafe(config.getOverlayRules()));
        return view;
    }

    private Object parseJsonSafe(String json) {
        if (!StringUtils.hasText(json)) return java.util.Collections.emptyList();
        try {
            return mapper.readValue(json, Object.class);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    private Path resolveProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        Path parent = current.getParent();
        return parent == null ? current : parent;
    }
}
