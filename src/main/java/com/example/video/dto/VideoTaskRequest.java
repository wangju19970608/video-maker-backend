package com.example.video.dto;

public class VideoTaskRequest {
    private Long templateId;
    private String name;
    private String age;
    private String time;
    private String hotel;
    private Long orderId;
    /**
     * 动态表单字段值，key 对应 TemplateConfig.formFields 中的 field.key。
     * 用于新版动态模板，老模板依然使用 name/age/time/hotel 兼容字段。
     */
    private java.util.Map<String, String> dynamicFields;

    public java.util.Map<String, String> getDynamicFields() { return dynamicFields; }
    public void setDynamicFields(java.util.Map<String, String> dynamicFields) { this.dynamicFields = dynamicFields; }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getHotel() {
        return hotel;
    }

    public void setHotel(String hotel) {
        this.hotel = hotel;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
