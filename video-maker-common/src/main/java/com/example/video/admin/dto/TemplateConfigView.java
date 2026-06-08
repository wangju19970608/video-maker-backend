package com.example.video.admin.dto;

import java.util.List;

/**
 * 模板配置视图：返回给前端的 formFields + overlayRules（已解析为对象）。
 */
public class TemplateConfigView {

    private Long templateId;
    private String analysisStatus;
    private String analysisError;
    private Object formFields;    // JSON 数组，反序列化后的 List
    private Object overlayRules;  // JSON 数组，反序列化后的 List

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }

    public String getAnalysisError() { return analysisError; }
    public void setAnalysisError(String analysisError) { this.analysisError = analysisError; }

    public Object getFormFields() { return formFields; }
    public void setFormFields(Object formFields) { this.formFields = formFields; }

    public Object getOverlayRules() { return overlayRules; }
    public void setOverlayRules(Object overlayRules) { this.overlayRules = overlayRules; }
}
