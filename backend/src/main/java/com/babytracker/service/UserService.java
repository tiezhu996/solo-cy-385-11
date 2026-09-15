package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.ErrorCode;
import com.babytracker.entity.AppUser;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.AppUserMapper;
import com.babytracker.utils.JwtUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {
    private final AppUserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserService(AppUserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(String nickname) {
        String name = requireNickname(nickname);
        AppUser user = new AppUser();
        user.setNickname(name);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 昵称唯一约束兜底：并发注册同名时只有一个成功
            throw new BizException(ErrorCode.NICKNAME_TAKEN, "昵称已被使用");
        }
        return tokenPayload(user);
    }

    public Map<String, Object> login(String nickname) {
        String name = requireNickname(nickname);
        AppUser user = userMapper.selectOne(new QueryWrapper<AppUser>().eq("nickname", name));
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在，请先注册");
        return tokenPayload(user);
    }

    public AppUser getById(Long userId) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.UNAUTHORIZED, "账号不存在");
        return user;
    }

    private String requireNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "昵称不能为空");
        }
        return nickname.trim();
    }

    private Map<String, Object> tokenPayload(AppUser user) {
        return Map.of(
                "token", jwtUtil.createToken(user.getId(), user.getNickname()),
                "user", Map.of("id", user.getId(), "nickname", user.getNickname()));
    }
}
