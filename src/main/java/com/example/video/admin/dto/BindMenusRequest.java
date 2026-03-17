package com.example.video.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class BindMenusRequest {

    private List<Long> menuIds = new ArrayList<>();

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}