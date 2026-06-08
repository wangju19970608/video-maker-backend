package com.example.video.controller;

import com.example.video.dto.WxLoginRequest;
import com.example.video.dto.WxLoginResponse;
import com.example.video.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/wx-login")
    public Result<WxLoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        try {
            log.info("wx-login request received, codePresent={}, nicknamePresent={}, avatarPresent={}",
                    request != null && request.getCode() != null && !request.getCode().trim().isEmpty(),
                    request != null && request.getNickname() != null && !request.getNickname().trim().isEmpty(),
                    request != null && request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty());
            WxLoginResponse response = sysUserService.wxLogin(request);
            log.info("wx-login success, userId={}, openid={}", response.getUserId(), response.getOpenid());
            return Result.success(response);
        } catch (Exception e) {
            log.error("wx-login failed, message={}", e.getMessage(), e);
            return Result.fail(e.getMessage());
        }
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

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
