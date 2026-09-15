package com.babytracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.babytracker.entity.AppUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AppUserMapper extends BaseMapper<AppUser> {

    /** 原子设置初始密码：仅当账号尚无密码时生效，并发下只有一个请求成功。 */
    @Update("UPDATE app_user SET password_hash = #{hash} WHERE id = #{id} AND password_hash IS NULL")
    int setPasswordIfAbsent(@Param("id") Long id, @Param("hash") String hash);
}
