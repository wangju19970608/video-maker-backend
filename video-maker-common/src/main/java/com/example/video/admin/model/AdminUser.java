package com.example.video.admin.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "admin_user")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(length = 128)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false)
    private Integer status = 1;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "admin_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<AdminRole> roles = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "wechat_qr_url", length = 255)
    private String wechatQrUrl;

    @Column(name = "wechat_link", length = 255)
    private String wechatLink;

    @Column(name = "customer_service_active")
    private Boolean customerServiceActive = false;

    @Column(name = "customer_service_weight")
    private Integer customerServiceWeight = 1;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Set<AdminRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<AdminRole> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getWechatQrUrl() {
        return wechatQrUrl;
    }

    public void setWechatQrUrl(String wechatQrUrl) {
        this.wechatQrUrl = wechatQrUrl;
    }

    public String getWechatLink() {
        return wechatLink;
    }

    public void setWechatLink(String wechatLink) {
        this.wechatLink = wechatLink;
    }

    public Boolean getCustomerServiceActive() {
        return customerServiceActive;
    }

    public void setCustomerServiceActive(Boolean customerServiceActive) {
        this.customerServiceActive = customerServiceActive;
    }

    public Integer getCustomerServiceWeight() {
        return customerServiceWeight;
    }

    public void setCustomerServiceWeight(Integer customerServiceWeight) {
        this.customerServiceWeight = customerServiceWeight;
    }
}