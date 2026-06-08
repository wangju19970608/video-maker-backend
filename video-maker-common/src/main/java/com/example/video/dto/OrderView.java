package com.example.video.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderView {
    private Long id;
    private String createdAt;
    private String productId;
    private String orderNo;
    private String status;
    private BigDecimal amount;
    private String customerName;
    private String customerPhone;
    private String remark;
    private String paidAt;
    private String taskId;
    private Integer maxGenerateCount;
    private Integer usedGenerateCount;
    private TemplateView template;
    private List<HistoricalTaskView> historicalTasks;

    public List<HistoricalTaskView> getHistoricalTasks() {
        return historicalTasks;
    }

    public void setHistoricalTasks(List<HistoricalTaskView> historicalTasks) {
        this.historicalTasks = historicalTasks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Integer getMaxGenerateCount() {
        return maxGenerateCount;
    }

    public void setMaxGenerateCount(Integer maxGenerateCount) {
        this.maxGenerateCount = maxGenerateCount;
    }

    public Integer getUsedGenerateCount() {
        return usedGenerateCount;
    }

    public void setUsedGenerateCount(Integer usedGenerateCount) {
        this.usedGenerateCount = usedGenerateCount;
    }

    public TemplateView getTemplate() {
        return template;
    }

    public void setTemplate(TemplateView template) {
        this.template = template;
    }
}