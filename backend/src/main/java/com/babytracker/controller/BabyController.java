package com.babytracker.controller;

import com.babytracker.dto.BabyView;
import com.babytracker.entity.Baby;
import com.babytracker.entity.BabyMember;
import com.babytracker.service.BabyService;
import com.babytracker.service.FamilyService;
import com.babytracker.utils.AuthContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/babies")
public class BabyController {
    private final BabyService service;
    private final FamilyService familyService;

    public BabyController(BabyService service, FamilyService familyService) {
        this.service = service;
        this.familyService = familyService;
    }

    @GetMapping
    public List<BabyView> list() {
        return service.listMine(AuthContext.requireUserId());
    }

    @PostMapping
    public Baby create(@RequestBody Baby baby) {
        return service.create(baby, AuthContext.requireUserId());
    }

    /** 没有创建者的旧宝宝（升级前数据），登录用户可查看并认领。 */
    @GetMapping("/adoptable")
    public List<Baby> adoptable() {
        return service.listAdoptable();
    }

    @GetMapping("/{babyId}")
    public Baby detail(@PathVariable("babyId") Long babyId) {
        return service.getForMember(babyId, AuthContext.requireUserId());
    }

    @PutMapping("/{babyId}")
    public Baby update(@PathVariable("babyId") Long babyId, @RequestBody Baby baby) {
        return service.update(babyId, baby, AuthContext.requireUserId());
    }

    /** 认领旧宝宝：认领人成为创建者（OWNER）。 */
    @PostMapping("/{babyId}/adopt")
    public BabyMember adopt(@PathVariable("babyId") Long babyId) {
        return familyService.adopt(babyId, AuthContext.requireUserId());
    }
}
