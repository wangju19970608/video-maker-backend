package com.example.video.admin.controller;

import com.example.video.admin.dto.AdminUserDto;
import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.UserUpsertRequest;
import com.example.video.admin.service.AdminUserService;
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
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserDto>> listUsers(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.listUsers(keyword, status));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDto> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userService.toDto(userService.findById(userId)));
    }

    @PostMapping
    public ApiResponse<AdminUserDto> createUser(@RequestBody UserUpsertRequest request) {
        return ApiResponse.success("created", userService.createUser(request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<AdminUserDto> updateUser(@PathVariable Long userId,
                                                @RequestBody UserUpsertRequest request) {
        return ApiResponse.success("updated", userService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Boolean> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.success(true);
    }
}
