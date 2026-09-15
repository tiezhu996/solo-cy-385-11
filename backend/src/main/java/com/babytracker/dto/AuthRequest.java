package com.babytracker.dto;

import lombok.Data;

/** 注册 / 登录请求体。 */
@Data
public class AuthRequest {
    private String nickname;
}
