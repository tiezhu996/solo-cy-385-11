package com.babytracker.dto;

import lombok.Data;

/** 领取邀请码请求体。 */
@Data
public class ClaimInviteRequest {
    private String code;
}
