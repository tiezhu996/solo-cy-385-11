package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.Baby;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface BabyMapper extends BaseMapper<Baby> {

    /** 对宝宝行加排他锁，用于串行化同一宝宝的成员变更，保证并发下结果唯一确定。 */
    @Select("SELECT id FROM baby WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);

    /** 没有任何 OWNER 成员的宝宝（升级前的旧数据），等待认领。 */
    @Select("SELECT * FROM baby WHERE id NOT IN (SELECT baby_id FROM baby_member WHERE role = 'OWNER')")
    List<Baby> selectOwnerless();
}
