package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("vaccine_record")
public class VaccineRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private String vaccineName;
    private LocalDate plannedDate;
    private Boolean completed;
}
