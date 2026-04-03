package com.example.video.admin.dto;

/**
 * 管理员手动保存/更新模板配置的请求体。
 * formFields 和 overlayRules 均为 JSON 字符串。
 */
public class TemplateConfigSaveRequest {
    /** JSON 数组字符串，表单字段配置 */
    private String formFields;
    /** JSON 数组字符串，视频叠加规则 */
    private String overlayRules;

    public String getFormFields() { return formFields; }
    public void setFormFields(String formFields) { this.formFields = formFields; }

    public String getOverlayRules() { return overlayRules; }
    public void setOverlayRules(String overlayRules) { this.overlayRules = overlayRules; }
}
