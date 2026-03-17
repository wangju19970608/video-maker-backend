package com.example.video.admin.controller;

import com.example.video.admin.dto.ApiResponse;
import com.example.video.admin.dto.LoginRequest;
import com.example.video.admin.dto.LoginResponse;
import com.example.video.admin.security.AdminAuthInterceptor;
import com.example.video.admin.security.AdminSession;
import com.example.video.admin.service.AdminAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/sessions")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/current")
    public ApiResponse<LoginResponse> me(HttpServletRequest request,
                                         @RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestHeader(value = "X-Admin-Token", required = false) String headerToken) {
        AdminSession session = (AdminSession) request.getAttribute(AdminAuthInterceptor.SESSION_ATTR);
        if (session == null) {
            return ApiResponse.fail("Unauthorized");
        }
        String token = resolveToken(authorization, headerToken);
        return ApiResponse.success(authService.me(session.getUserId(), token));
    }

    @DeleteMapping("/current")
    public ApiResponse<Boolean> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestHeader(value = "X-Admin-Token", required = false) String headerToken) {
        String token = resolveToken(authorization, headerToken);
        authService.logout(token);
        return ApiResponse.success(true);
    }

    private String resolveToken(String authorization, String headerToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return headerToken;
    }
}
