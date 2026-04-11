package com.example.video.dto;

public class WxLoginResponse {

    private String token;
    private Long userId;
    private String openid;
    private String nickname;
    private String avatarUrl;

    public WxLoginResponse() {}

    public WxLoginResponse(String token, Long userId, String openid, String nickname, String avatarUrl) {
        this.token = token;
        this.userId = userId;
        this.openid = openid;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}