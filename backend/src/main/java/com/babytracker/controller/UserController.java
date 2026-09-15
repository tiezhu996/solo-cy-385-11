package com.babytracker.controller;

import com.babytracker.dto.AuthRequest;
import com.babytracker.dto.ChangePasswordRequest;
import com.babytracker.entity.AppUser;
import com.babytracker.service.UserService;
import com.babytracker.utils.AuthContext;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody AuthRequest request) {
        return userService.register(request.getNickname(), request.getPassword());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        return userService.login(request.getNickname(), request.getPassword());
    }

    /** 旧账号（无密码）设置初始密码，公开接口但仅对无密码账号生效一次。 */
    @PostMapping("/set-password")
    public Map<String, Object> setInitialPassword(@RequestBody AuthRequest request) {
        return userService.setInitialPassword(request.getNickname(), request.getPassword());
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(AuthContext.requireUserId(), request.getOldPassword(), request.getNewPassword());
        return Map.of("success", true);
    }

    @GetMapping("/me")
    public AppUser me() {
        return userService.getById(AuthContext.requireUserId());
    }
}
