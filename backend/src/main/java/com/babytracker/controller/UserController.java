package com.babytracker.controller;

import com.babytracker.dto.AuthRequest;
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
        return userService.register(request.getNickname());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        return userService.login(request.getNickname());
    }

    @GetMapping("/me")
    public AppUser me() {
        return userService.getById(AuthContext.requireUserId());
    }
}
