package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String nickname;
    private LocalDateTime createdAt;
}
