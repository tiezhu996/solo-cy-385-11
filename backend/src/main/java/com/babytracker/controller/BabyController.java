package com.babytracker.controller;

import com.babytracker.dto.BabyView;
import com.babytracker.entity.Baby;
import com.babytracker.service.BabyService;
import com.babytracker.utils.AuthContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/babies")
public class BabyController {
    private final BabyService service;

    public BabyController(BabyService service) { this.service = service; }

    @GetMapping
    public List<BabyView> list() {
        return service.listMine(AuthContext.requireUserId());
    }

    @PostMapping
    public Baby create(@RequestBody Baby baby) {
        return service.create(baby, AuthContext.requireUserId());
    }

    @GetMapping("/{babyId}")
    public Baby detail(@PathVariable("babyId") Long babyId) {
        return service.getForMember(babyId, AuthContext.requireUserId());
    }

    @PutMapping("/{babyId}")
    public Baby update(@PathVariable("babyId") Long babyId, @RequestBody Baby baby) {
        return service.update(babyId, baby, AuthContext.requireUserId());
    }
}
