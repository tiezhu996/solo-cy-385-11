package com.babytracker.controller;

import com.babytracker.constants.FamilyEnums;
import com.babytracker.entity.GrowthRecord;
import com.babytracker.service.FamilyService;
import com.babytracker.service.GrowthService;
import com.babytracker.utils.AuthContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 生长记录：查看需 VIEW 及以上，写入需 RECORD 及以上，每次请求实时校验成员角色。 */
@RestController
@RequestMapping("/api/babies/{babyId}/growth")
public class GrowthController {
    private final GrowthService service;
    private final FamilyService familyService;

    public GrowthController(GrowthService service, FamilyService familyService) {
        this.service = service;
        this.familyService = familyService;
    }

    @GetMapping
    public List<GrowthRecord> list(@PathVariable("babyId") Long babyId) {
        familyService.requireRole(babyId, AuthContext.requireUserId(), FamilyEnums.ROLE_VIEW);
        return service.list(babyId);
    }

    @PostMapping
    public GrowthRecord record(@PathVariable("babyId") Long babyId, @RequestBody GrowthRecord record) {
        familyService.requireRole(babyId, AuthContext.requireUserId(), FamilyEnums.ROLE_RECORD);
        record.setBabyId(babyId);
        return service.record(record);
    }
}
