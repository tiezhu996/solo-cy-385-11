package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.entity.VaccineRecord;
import com.babytracker.mapper.VaccineMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VaccineService {
    private final VaccineMapper mapper;

    public VaccineService(VaccineMapper mapper) { this.mapper = mapper; }

    public List<VaccineRecord> schedule(Long babyId) {
        return mapper.selectList(new QueryWrapper<VaccineRecord>()
                .eq("baby_id", babyId).orderByAsc("planned_date"));
    }

    public VaccineRecord save(VaccineRecord record) {
        record.setId(null);
        mapper.insert(record);
        return record;
    }
}
