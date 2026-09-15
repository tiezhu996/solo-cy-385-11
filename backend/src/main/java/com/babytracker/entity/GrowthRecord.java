package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("growth_record")
public class GrowthRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private LocalDate recordedAt;
    private Double heightCm;
    private Double weightKg;
    private String percentile;

    public Double getWeightKg() {
        return weightKg;
    }

    public void setPercentile(String percentile) {
        this.percentile = percentile;
    }
}
