package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.BabyInvite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface BabyInviteMapper extends BaseMapper<BabyInvite> {

    /**
     * 原子领取：仅当邀请码仍处于 ACTIVE 且未过期时置为 CLAIMED。
     * 并发领取时数据库保证只有一个请求更新成功（返回 1），其余返回 0。
     */
    @Update("UPDATE baby_invite SET status = 'CLAIMED', claimed_by = #{userId}, claimed_at = NOW() "
            + "WHERE id = #{id} AND status = 'ACTIVE' AND expires_at > NOW()")
    int claimIfActive(@Param("id") Long id, @Param("userId") Long userId);

    /** 原子撤销：仅 ACTIVE 状态可撤销，返回 0 表示已被领取或已撤销。 */
    @Update("UPDATE baby_invite SET status = 'REVOKED' WHERE id = #{id} AND baby_id = #{babyId} AND status = 'ACTIVE'")
    int revokeIfActive(@Param("id") Long id, @Param("babyId") Long babyId);
}
