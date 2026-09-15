package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.Baby;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BabyMapper extends BaseMapper<Baby> {

    /** 对宝宝行加排他锁，用于串行化同一宝宝的成员变更，保证并发下结果唯一确定。 */
    @Select("SELECT id FROM baby WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);
}
