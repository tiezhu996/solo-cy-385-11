package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.ErrorCode;
import com.babytracker.entity.AppUser;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.AppUserMapper;
import com.babytracker.utils.JwtUtil;
import com.babytracker.utils.PasswordUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final AppUserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserService(AppUserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(String nickname, String password) {
        String name = requireNickname(nickname);
        requirePassword(password);
        AppUser user = new AppUser();
        user.setNickname(name);
        user.setPasswordHash(PasswordUtil.hash(password));
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 昵称唯一约束兜底：并发注册同名时只有一个成功
            throw new BizException(ErrorCode.NICKNAME_TAKEN, "昵称已被使用");
        }
        return tokenPayload(user);
    }

    /** 登录必须同时校验昵称与密码，任一不符都返回同一个 401，避免探测账号是否存在。 */
    public Map<String, Object> login(String nickname, String password) {
        String name = requireNickname(nickname);
        if (password == null || password.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "密码不能为空");
        }
        AppUser user = userMapper.selectOne(new QueryWrapper<AppUser>().eq("nickname", name));
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "昵称或密码错误");
        }
        if (user.getPasswordHash() == null) {
            throw new BizException(ErrorCode.PASSWORD_NOT_SET, "该账号尚未设置密码，请先设置初始密码");
        }
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "昵称或密码错误");
        }
        return tokenPayload(user);
    }

    /** 为没有密码的旧账号设置初始密码：原子更新，仅当 password_hash 为空时生效一次。 */
    public Map<String, Object> setInitialPassword(String nickname, String password) {
        String name = requireNickname(nickname);
        requirePassword(password);
        AppUser user = userMapper.selectOne(new QueryWrapper<AppUser>().eq("nickname", name));
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        int updated = userMapper.setPasswordIfAbsent(user.getId(), PasswordUtil.hash(password));
        if (updated == 0) {
            throw new BizException(ErrorCode.PASSWORD_ALREADY_SET, "账号已设置密码，请直接登录");
        }
        return tokenPayload(user);
    }

    /** 修改密码：已设置密码的账号必须校验原密码。 */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        requirePassword(newPassword);
        AppUser user = getById(userId);
        if (user.getPasswordHash() != null
                && (oldPassword == null || !PasswordUtil.verify(oldPassword, user.getPasswordHash()))) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "原密码错误");
        }
        user.setPasswordHash(PasswordUtil.hash(newPassword));
        userMapper.updateById(user);
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

    private void requirePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "密码长度至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
    }

    private Map<String, Object> tokenPayload(AppUser user) {
        return Map.of(
                "token", jwtUtil.createToken(user.getId(), user.getNickname()),
                "user", Map.of("id", user.getId(), "nickname", user.getNickname()));
    }
}
