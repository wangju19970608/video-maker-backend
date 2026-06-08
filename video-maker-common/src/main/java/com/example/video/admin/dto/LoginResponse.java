package com.example.video.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class LoginResponse {

    private String token;
    private AdminUserDto user;
    private List<MenuNodeDto> menus = new ArrayList<>();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AdminUserDto getUser() {
        return user;
    }

    public void setUser(AdminUserDto user) {
        this.user = user;
    }

    public List<MenuNodeDto> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuNodeDto> menus) {
        this.menus = menus;
    }
}