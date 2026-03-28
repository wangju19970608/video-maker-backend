package com.example.video.dto;

public class PaymentQrcodeResponse {
    private String qrcodeUrl;
    private String orderNo;
    private Object amount;

    public PaymentQrcodeResponse() {}

    public PaymentQrcodeResponse(String qrcodeUrl, String orderNo, Object amount) {
        this.qrcodeUrl = qrcodeUrl;
        this.orderNo = orderNo;
        this.amount = amount;
    }

    public String getQrcodeUrl() {
        return qrcodeUrl;
    }

    public void setQrcodeUrl(String qrcodeUrl) {
        this.qrcodeUrl = qrcodeUrl;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Object getAmount() {
        return amount;
    }

    public void setAmount(Object amount) {
        this.amount = amount;
    }
}