package com.babytracker.dto;

import lombok.Data;

/** 生成邀请码请求体：role 为 VIEW/RECORD/MANAGE，ttlMinutes 缺省 72 小时。 */
@Data
public class CreateInviteRequest {
    private String role;
    private Long ttlMinutes;
}
