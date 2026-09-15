package com.babytracker.utils;

import com.babytracker.constants.ErrorCode;
import com.babytracker.exception.BizException;

/** 当前请求登录用户上下文，由 AuthInterceptor 写入与清理。 */
public final class AuthContext {
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(Long userId) { CURRENT_USER.set(userId); }

    public static Long requireUserId() {
        Long userId = CURRENT_USER.get();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        return userId;
    }

    public static void clear() { CURRENT_USER.remove(); }
}
