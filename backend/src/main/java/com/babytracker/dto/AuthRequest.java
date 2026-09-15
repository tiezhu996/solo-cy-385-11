package com.babytracker.dto;

import lombok.Data;

/** 注册 / 登录 / 设置初始密码请求体。 */
@Data
public class AuthRequest {
    private String nickname;
    private String password;
}
