package com.example.video.admin.dto;

import java.math.BigDecimal;

public class OrderStatusRequest {

    private String status;
    private BigDecimal amount;

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
}