package com.example.video.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 模板动态配置：存储 AI 分析后生成的表单字段和视频叠加规则。
 *
 * formFields JSON 示例：
 * [
 *   {"key":"name","type":"text","label":"宝宝姓名","placeholder":"请输入姓名","required":true},
 *   {"key":"age","type":"number","label":"年龄","placeholder":"例如：4","required":true},
 *   {"key":"blessing","type":"text","label":"祝福语","placeholder":"输入送上的祝福词","required":false},
 *   {"key":"photo","type":"image","label":"上传照片","required":false}
 * ]
 *
 * overlayRules JSON 示例：
 * [
 *   {"key":"name","type":"text","startTime":2.0,"endTime":20.0,"x":0.60,"y":0.28,"fontSize":80,"fontColor":"#ffffff"},
 *   {"key":"age","type":"text","startTime":2.0,"endTime":20.0,"x":0.625,"y":0.42,"fontSize":160,"fontColor":"#ffffff"},
 *   {"key":"blessing","type":"text","startTime":2.0,"endTime":20.0,"x":0.60,"y":0.65,"fontSize":40,"fontColor":"#ffffff"},
 *   {"key":"photo","type":"image","startTime":5.0,"endTime":15.0,"x":0.0,"y":0.0,"width":1.0,"height":1.0}
 * ]
 *
 * 坐标 x/y/width/height 均为 0.0~1.0 的相对比例，合成时乘以实际分辨率。
 */
@Entity
@Table(name = "template_config")
public class TemplateConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false, unique = true)
    private VideoTemplate template;

    /** 动态表单字段列表，JSON 数组字符串 */
    @Column(name = "form_fields", columnDefinition = "TEXT")
    private String formFields;

    /** 视频叠加规则列表，JSON 数组字符串 */
    @Column(name = "overlay_rules", columnDefinition = "TEXT")
    private String overlayRules;

    /** AI 分析状态：pending / analysing / done / failed */
    @Column(name = "analysis_status", length = 16)
    private String analysisStatus = "pending";

    /** AI 分析失败时的错误信息 */
    @Column(name = "analysis_error", length = 500)
    private String analysisError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VideoTemplate getTemplate() { return template; }
    public void setTemplate(VideoTemplate template) { this.template = template; }

    public String getFormFields() { return formFields; }
    public void setFormFields(String formFields) { this.formFields = formFields; }

    public String getOverlayRules() { return overlayRules; }
    public void setOverlayRules(String overlayRules) { this.overlayRules = overlayRules; }

    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }

    public String getAnalysisError() { return analysisError; }
    public void setAnalysisError(String analysisError) { this.analysisError = analysisError; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
