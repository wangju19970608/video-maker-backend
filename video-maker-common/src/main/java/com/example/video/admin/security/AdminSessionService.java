package com.example.video.admin.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminSessionService {

    private static final int SESSION_HOURS = 12;
    private final Map<String, AdminSession> tokenStore = new ConcurrentHashMap<>();

    public String createToken(Long userId, String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        AdminSession session = new AdminSession();
        session.setUserId(userId);
        session.setUsername(username);
        session.setExpireAt(LocalDateTime.now().plusHours(SESSION_HOURS));
        tokenStore.put(token, session);
        return token;
    }

    public AdminSession verify(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        AdminSession session = tokenStore.get(token);
        if (session == null) {
            return null;
        }
        if (session.getExpireAt().isBefore(LocalDateTime.now())) {
            tokenStore.remove(token);
            return null;
        }
        return session;
    }

    public void remove(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}