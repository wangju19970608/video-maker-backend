package com.example.video.security;

import com.example.video.model.SysUser;
import com.example.video.repository.SysUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTR = "USER";

    // Token format: {32位UUID无横线}{纯数字userId}
    // 使用固定长度截取：前32位是UUID，剩余全部是userId数字
    // 注意：不能用正则 ([a-f0-9]{32})(\d+)，因为UUID中的0-9和userId的数字会被贪婪匹配混淆
    private static final int UUID_LENGTH = 32;

    private final SysUserRepository sysUserRepository;

    public UserAuthInterceptor(SysUserRepository sysUserRepository) {
        this.sysUserRepository = sysUserRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("[UserAuthInterceptor] {} {}", method, path);

        // Let CORS preflight pass directly
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            log.warn("[UserAuthInterceptor] Missing token for {} {}", method, path);
            unauthorized(response, "Missing token");
            return false;
        }

        Long userId = extractUserId(token);
        if (userId == null) {
            log.warn("[UserAuthInterceptor] Invalid token format for {} {}, token prefix={}", method, path,
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);
            unauthorized(response, "Invalid token");
            return false;
        }

        log.debug("[UserAuthInterceptor] Token valid, looking up userId={}", userId);
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[UserAuthInterceptor] User not found in DB, userId={}", userId);
            unauthorized(response, "User not found");
            return false;
        }

        log.debug("[UserAuthInterceptor] Auth passed, userId={}, openid={}", user.getId(), user.getOpenid());
        request.setAttribute(USER_ATTR, user);
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    /**
     * 从token中提取userId。
     * Token格式: {32位UUID无横线}{userId纯数字}，例如 "a1b2c3d4...32chars...123"
     * 修复Bug: 原正则 ([a-f0-9]{32})(\d+) 存在歧义，因为0-9既是十六进制也是数字，
     * 正则引擎贪婪匹配时会将userId数字字符(0-9)也纳入第一个捕获组，导致第二组为空。
     * 修复方案: 按固定长度(32)截取UUID部分，剩余部分解析为userId。
     */
    private Long extractUserId(String token) {
        try {
            if (token.length() > UUID_LENGTH) {
                String userIdPart = token.substring(UUID_LENGTH);
                // 验证userId部分是纯数字
                if (userIdPart.matches("\\d+")) {
                    return Long.parseLong(userIdPart);
                }
                log.warn("[UserAuthInterceptor] UserId part is not numeric: '{}'", userIdPart);
            } else {
                log.warn("[UserAuthInterceptor] Token too short, length={} (expected > {})", token.length(), UUID_LENGTH);
            }
        } catch (NumberFormatException e) {
            log.warn("[UserAuthInterceptor] Failed to parse userId from token: {}", e.getMessage());
        }
        return null;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}