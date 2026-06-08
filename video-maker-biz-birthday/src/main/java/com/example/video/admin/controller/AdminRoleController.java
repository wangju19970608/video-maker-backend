package com.example.video.admin.controller;

import com.example.video.admin.dto.AdminRoleDto;
import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.RoleUpsertRequest;
import com.example.video.admin.service.AdminRoleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    private final AdminRoleService roleService;

    public AdminRoleController(AdminRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<AdminRoleDto>> listRoles() {
        return ApiResponse.success(roleService.listAll());
    }

    @GetMapping("/{roleId}")
    public ApiResponse<AdminRoleDto> getRole(@PathVariable Long roleId) {
        return ApiResponse.success(roleService.toDto(roleService.findById(roleId)));
    }

    @PostMapping
    public ApiResponse<AdminRoleDto> createRole(@RequestBody RoleUpsertRequest request) {
        return ApiResponse.success("created", roleService.create(request));
    }

    @PutMapping("/{roleId}")
    public ApiResponse<AdminRoleDto> updateRole(@PathVariable Long roleId,
                                                @RequestBody RoleUpsertRequest request) {
        return ApiResponse.success("updated", roleService.update(roleId, request));
    }

    @DeleteMapping("/{roleId}")
    public ApiResponse<Boolean> deleteRole(@PathVariable Long roleId) {
        roleService.delete(roleId);
        return ApiResponse.success(true);
    }
}
