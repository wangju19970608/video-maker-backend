package com.example.video.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.example.video.dto.WxLoginRequest;
import com.example.video.dto.WxLoginResponse;
import com.example.video.model.SysUser;
import com.example.video.repository.SysUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SysUserService {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private SysUserRepository sysUserRepository;

    public WxLoginResponse wxLogin(WxLoginRequest request) {
        try {
            if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
                throw new IllegalArgumentException("微信登录code不能为空");
            }

            log.info("wx-login start, codeLen={}, nicknamePresent={}, avatarPresent={}",
                    request.getCode().length(),
                    request.getNickname() != null && !request.getNickname().trim().isEmpty(),
                    request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty());

            WxMaJscode2SessionResult sessionResult = wxMaService.getUserService().getSessionInfo(request.getCode());
            String openid = sessionResult.getOpenid();
            String sessionKey = sessionResult.getSessionKey();
            String unionid = sessionResult.getUnionid();

            log.info("wx code2session success, openid={}, unionidPresent={}, sessionKeyPresent={}",
                    openid,
                    unionid != null && !unionid.isEmpty(),
                    sessionKey != null && !sessionKey.isEmpty());

            if (openid == null || openid.isEmpty()) {
                throw new RuntimeException("获取openid失败");
            }

            SysUser user = findOrCreateUser(openid, unionid, request.getNickname(), request.getAvatarUrl(), sessionKey);
            String token = generateToken(user);

            log.info("wx-login db upsert success, userId={}, openid={}, status={}",
                    user.getId(), user.getOpenid(), user.getStatus());

            return new WxLoginResponse(token, user.getId(), user.getOpenid(),
                    user.getNickname(), user.getAvatarUrl());

        } catch (Exception e) {
            log.error("微信登录失败", e);
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }

    private SysUser findOrCreateUser(String openid, String unionid, String nickname,
                                     String avatarUrl, String sessionKey) {
        Optional<SysUser> existingUser = sysUserRepository.findByOpenid(openid);

        if (existingUser.isPresent()) {
            SysUser user = existingUser.get();
            if (nickname != null && !nickname.isEmpty()) {
                user.setNickname(nickname);
            }
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }
            user.setSessionKey(sessionKey);
            user.setLastLoginAt(LocalDateTime.now());
            return sysUserRepository.save(user);
        } else {
            SysUser newUser = new SysUser();
            newUser.setOpenid(openid);
            newUser.setUnionid(unionid);
            newUser.setNickname(nickname);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setSessionKey(sessionKey);
            newUser.setStatus(1);
            newUser.setLastLoginAt(LocalDateTime.now());
            return sysUserRepository.save(newUser);
        }
    }

    private String generateToken(SysUser user) {
        return UUID.randomUUID().toString().replace("-", "") + user.getId();
    }

    public SysUser getUserById(Long id) {
        return sysUserRepository.findById(id).orElse(null);
    }

    public SysUser getUserByOpenid(String openid) {
        return sysUserRepository.findByOpenid(openid).orElse(null);
    }
}
