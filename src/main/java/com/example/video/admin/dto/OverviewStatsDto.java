package com.example.video.admin.dto;

import java.math.BigDecimal;

public class OverviewStatsDto {

    private BigDecimal todaySalesAmount;
    private Long todayPaidOrders;
    private Long todayOrders;
    private BigDecimal totalSalesAmount;
    private Long totalPaidOrders;
    private Long totalOrders;
    private Long totalUsers;
    private Long totalTemplates;
    private Long onSaleTemplates;
    private Long pendingOrders;

    public BigDecimal getTodaySalesAmount() {
        return todaySalesAmount;
    }

    public void setTodaySalesAmount(BigDecimal todaySalesAmount) {
        this.todaySalesAmount = todaySalesAmount;
    }

    public Long getTodayPaidOrders() {
        return todayPaidOrders;
    }

    public void setTodayPaidOrders(Long todayPaidOrders) {
        this.todayPaidOrders = todayPaidOrders;
    }

    public Long getTodayOrders() {
        return todayOrders;
    }

    public void setTodayOrders(Long todayOrders) {
        this.todayOrders = todayOrders;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public Long getTotalPaidOrders() {
        return totalPaidOrders;
    }

    public void setTotalPaidOrders(Long totalPaidOrders) {
        this.totalPaidOrders = totalPaidOrders;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalTemplates() {
        return totalTemplates;
    }

    public void setTotalTemplates(Long totalTemplates) {
        this.totalTemplates = totalTemplates;
    }

    public Long getOnSaleTemplates() {
        return onSaleTemplates;
    }

    public void setOnSaleTemplates(Long onSaleTemplates) {
        this.onSaleTemplates = onSaleTemplates;
    }

    public Long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(Long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }
}