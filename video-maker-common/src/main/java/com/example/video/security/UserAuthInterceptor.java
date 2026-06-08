package com.example.video.security;

import com.example.video.model.SysUser;
import com.example.video.repository.SysUserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTR = "USER";

    // Token format: UUID(without dash) + userId
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^([a-f0-9]{32})(\\d+)$");

    private final SysUserRepository sysUserRepository;

    public UserAuthInterceptor(SysUserRepository sysUserRepository) {
        this.sysUserRepository = sysUserRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Let CORS preflight pass directly
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            unauthorized(response, "Missing token");
            return false;
        }

        Long userId = extractUserId(token);
        if (userId == null) {
            unauthorized(response, "Invalid token");
            return false;
        }

        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            unauthorized(response, "User not found");
            return false;
        }

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

    private Long extractUserId(String token) {
        try {
            Matcher matcher = TOKEN_PATTERN.matcher(token);
            if (matcher.matches()) {
                return Long.parseLong(matcher.group(2));
            }
            // Fallback: try to parse last digits as userId
            String numericPart = token.replaceAll("[a-f0-9]", "");
            if (!numericPart.isEmpty()) {
                return Long.parseLong(numericPart);
            }
        } catch (NumberFormatException e) {
            // ignore
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