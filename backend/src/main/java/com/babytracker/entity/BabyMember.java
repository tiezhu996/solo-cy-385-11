package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("baby_member")
public class BabyMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private Long userId;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 成员昵称，仅用于成员列表回读展示，不落库。 */
    @TableField(exist = false)
    private String nickname;
}
