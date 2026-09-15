package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("baby_invite")
public class BabyInvite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private String code;
    private String role;
    private String status;
    private LocalDateTime expiresAt;
    private Long createdBy;
    private Long claimedBy;
    private LocalDateTime claimedAt;
    private LocalDateTime createdAt;
}
