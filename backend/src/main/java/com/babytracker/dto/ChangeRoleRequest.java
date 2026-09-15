package com.babytracker.dto;

import lombok.Data;

/** 调整成员角色请求体：role 为 VIEW/RECORD/MANAGE。 */
@Data
public class ChangeRoleRequest {
    private String role;
}
