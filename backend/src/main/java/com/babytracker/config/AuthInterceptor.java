package com.babytracker.config;

import com.babytracker.constants.ErrorCode;
import com.babytracker.exception.BizException;
import com.babytracker.utils.AuthContext;
import com.babytracker.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 校验 Authorization: Bearer <token>，并将用户 ID 写入 AuthContext。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long userId = jwtUtil.parseUserId(header.substring(7));
            if (userId != null) {
                AuthContext.set(userId);
                return true;
            }
        }
        throw new BizException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
