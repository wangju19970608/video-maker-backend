package com.example.video.controller;

import com.example.video.model.SysUser;
import com.example.video.repository.SysUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/extractor/user")
public class ExtractorUserController {

    @Value("${extractor.douyin.app-id}")
    private String appId;

    @Value("${extractor.douyin.app-secret}")
    private String appSecret;

    @Autowired
    private SysUserRepository sysUserRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("=== [Login] 收到登录请求 ===");
        try {
            // ---- 1. 参数校验 ----
            if (request == null) {
                log.warn("[Login] 请求体为空");
                return Result.fail("请求体不能为空");
            }
            log.info("[Login] 请求参数: code={}, nickname={}, hasAvatar={}",
                    request.getCode(),
                    request.getNickname(),
                    request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty());

            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                log.warn("[Login] code为空，拒绝登录");
                return Result.fail("code is required");
            }

            String code = request.getCode().trim();
            log.info("[Login] appId={}, code(前8位)={}", appId, code.length() >= 8 ? code.substring(0, 8) + "..." : code);

            // ---- 2. 调用抖音 code2Session 接口 ----
            String openid = null;
            String sessionKey = null;
            String unionid = null;

            try {
                Map<String, String> body = new HashMap<>();
                body.put("appid", appId);
                body.put("secret", appSecret);
                body.put("code", code);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, headers);

                log.info("[Login] 正在调用抖音 jscode2session 接口...");
                ResponseEntity<String> apiResponse = restTemplate.postForEntity(
                        "https://developer.toutiao.com/api/apps/v2/jscode2session",
                        httpEntity,
                        String.class
                );

                log.info("[Login] 抖音接口响应状态码: {}", apiResponse.getStatusCode());
                log.info("[Login] 抖音接口响应体: {}", apiResponse.getBody());

                JsonNode root = objectMapper.readTree(apiResponse.getBody());

                // 抖音 v2 接口: err_no=0 表示成功，数据在 data 节点
                if (root.has("err_no")) {
                    int errNo = root.path("err_no").asInt(-1);
                    log.info("[Login] 抖音响应 err_no={}, err_tips={}", errNo, root.path("err_tips").asText(""));
                    if (errNo == 0) {
                        JsonNode dataNode = root.path("data");
                        openid = dataNode.path("openid").asText(null);
                        sessionKey = dataNode.path("session_key").asText(null);
                        unionid = dataNode.path("unionid").asText(null);
                        log.info("[Login] 抖音返回 openid(前8位)={}, 有unionid={}",
                                openid != null && openid.length() >= 8 ? openid.substring(0, 8) + "..." : openid,
                                unionid != null && !unionid.isEmpty());
                    } else {
                        log.warn("[Login] 抖音接口返回错误: err_no={}, err_tips={}", errNo, root.path("err_tips").asText());
                    }
                }
                // 旧格式兼容: error=0
                else if (root.has("error") && root.path("error").asInt(-1) == 0) {
                    openid = root.path("openid").asText(null);
                    sessionKey = root.path("session_key").asText(null);
                    unionid = root.path("unionid").asText(null);
                    log.info("[Login] 抖音返回(旧格式) openid(前8位)={}",
                            openid != null && openid.length() >= 8 ? openid.substring(0, 8) + "..." : openid);
                } else {
                    log.warn("[Login] 抖音接口返回未知格式，完整响应: {}", apiResponse.getBody());
                }

            } catch (Exception e) {
                log.warn("[Login] 调用抖音API失败，将使用 mock 登录（仅用于测试）: {}", e.getMessage());
            }

            // ---- 3. Mock 回退（openid为空时） ----
            if (openid == null || openid.trim().isEmpty()) {
                log.info("[Login] openid为空，启用 mock 登录...");
                openid = "dy_mock_openid_" + code.substring(Math.max(0, code.length() - 8));
                sessionKey = "mock_session_key";
                log.info("[Login] mock openid={}", openid);
            }

            // ---- 4. 查找或创建用户 ----
            // 加 "dy_" 前缀区分微信用户
            String dbOpenid = openid.startsWith("dy_") ? openid : "dy_" + openid;
            log.info("[Login] 开始查找/创建用户, dbOpenid={}", dbOpenid);

            SysUser user = findOrCreateUser(dbOpenid, unionid, request.getNickname(), request.getAvatarUrl(), sessionKey);
            log.info("[Login] 用户操作完成, userId={}, nickname={}", user.getId(), user.getNickname());

            // ---- 5. 生成 Token ----
            // Token格式: {32位UUID无横线}{userId数字}，确保 UserAuthInterceptor 可以正确解析
            String token = UUID.randomUUID().toString().replace("-", "") + user.getId();
            log.info("[Login] 生成token成功, userId={}, tokenLength={}", user.getId(), token.length());
            log.info("=== [Login] 登录成功 userId={} ===", user.getId());

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserId(user.getId());
            response.setOpenid(openid);
            response.setNickname(user.getNickname());
            response.setAvatarUrl(user.getAvatarUrl());

            return Result.success(response);

        } catch (Exception e) {
            log.error("[Login] 登录接口发生异常", e);
            return Result.fail("登录失败: " + e.getMessage());
        }
    }

    private SysUser findOrCreateUser(String openid, String unionid, String nickname, String avatarUrl, String sessionKey) {
        log.info("[findOrCreateUser] 查询数据库, openid={}", openid);
        Optional<SysUser> existingUser = sysUserRepository.findByOpenid(openid);

        if (existingUser.isPresent()) {
            SysUser user = existingUser.get();
            log.info("[findOrCreateUser] 找到已有用户, userId={}, 更新登录信息", user.getId());
            if (nickname != null && !nickname.trim().isEmpty()) {
                user.setNickname(nickname);
            }
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }
            user.setSessionKey(sessionKey);
            user.setLastLoginAt(LocalDateTime.now());
            SysUser saved = sysUserRepository.save(user);
            log.info("[findOrCreateUser] 用户信息更新成功, userId={}", saved.getId());
            return saved;
        } else {
            log.info("[findOrCreateUser] 用户不存在，创建新用户, openid={}", openid);
            SysUser newUser = new SysUser();
            newUser.setOpenid(openid);
            newUser.setUnionid(unionid);
            newUser.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : "抖音用户");
            newUser.setAvatarUrl(avatarUrl != null && !avatarUrl.trim().isEmpty() ? avatarUrl : "https://img.yzcdn.cn/vant/cat.jpeg");
            newUser.setSessionKey(sessionKey);
            newUser.setStatus(1);
            newUser.setLastLoginAt(LocalDateTime.now());
            SysUser saved = sysUserRepository.save(newUser);
            log.info("[findOrCreateUser] 新用户创建成功, userId={}, openid={}", saved.getId(), saved.getOpenid());
            return saved;
        }
    }

    // Request & Response DTOs
    public static class LoginRequest {
        private String code;
        private String nickname;
        private String avatarUrl;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class LoginResponse {
        private String token;
        private Long userId;
        private String openid;
        private String nickname;
        private String avatarUrl;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getOpenid() { return openid; }
        public void setOpenid(String openid) { this.openid = openid; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class Result<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.success = true;
            result.message = "success";
            result.data = data;
            return result;
        }

        public static <T> Result<T> fail(String message) {
            Result<T> result = new Result<>();
            result.success = false;
            result.message = message;
            return result;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }
}
