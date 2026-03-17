package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.MenuNodeDto;
import com.example.video.admin.dto.MenuUpsertRequest;
import com.example.video.admin.service.AdminMenuService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menus")
public class AdminMenuController {

    private final AdminMenuService menuService;

    public AdminMenuController(AdminMenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ApiResponse<List<MenuNodeDto>> listMenus(@RequestParam(defaultValue = "false") boolean enabledOnly) {
        return ApiResponse.success(menuService.listTree(enabledOnly));
    }

    @PostMapping
    public ApiResponse<MenuNodeDto> createMenu(@RequestBody MenuUpsertRequest request) {
        return ApiResponse.success("created", menuService.create(request));
    }

    @PutMapping("/{menuId}")
    public ApiResponse<MenuNodeDto> updateMenu(@PathVariable Long menuId,
                                               @RequestBody MenuUpsertRequest request) {
        return ApiResponse.success("updated", menuService.update(menuId, request));
    }

    @DeleteMapping("/{menuId}")
    public ApiResponse<Boolean> deleteMenu(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return ApiResponse.success(true);
    }
}