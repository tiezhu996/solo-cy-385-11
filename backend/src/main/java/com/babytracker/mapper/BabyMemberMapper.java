package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.BabyMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BabyMemberMapper extends BaseMapper<BabyMember> {

    /** 统计具备管理能力（OWNER/MANAGE）的成员数，需在持有宝宝行锁的事务内调用。 */
    @Select("SELECT COUNT(*) FROM baby_member WHERE baby_id = #{babyId} AND role IN ('OWNER','MANAGE')")
    long countManagers(@Param("babyId") Long babyId);
}
