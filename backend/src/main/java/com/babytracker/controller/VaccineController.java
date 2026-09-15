package com.babytracker.controller;

import com.babytracker.constants.FamilyEnums;
import com.babytracker.entity.VaccineRecord;
import com.babytracker.service.FamilyService;
import com.babytracker.service.VaccineService;
import com.babytracker.utils.AuthContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 疫苗计划：查看需 VIEW 及以上，登记需 RECORD 及以上，每次请求实时校验成员角色。 */
@RestController
@RequestMapping("/api/babies/{babyId}/vaccines")
public class VaccineController {
    private final VaccineService service;
    private final FamilyService familyService;

    public VaccineController(VaccineService service, FamilyService familyService) {
        this.service = service;
        this.familyService = familyService;
    }

    @GetMapping
    public List<VaccineRecord> schedule(@PathVariable("babyId") Long babyId) {
        familyService.requireRole(babyId, AuthContext.requireUserId(), FamilyEnums.ROLE_VIEW);
        return service.schedule(babyId);
    }

    @PostMapping
    public VaccineRecord save(@PathVariable("babyId") Long babyId, @RequestBody VaccineRecord record) {
        familyService.requireRole(babyId, AuthContext.requireUserId(), FamilyEnums.ROLE_RECORD);
        record.setBabyId(babyId);
        return service.save(record);
    }
}
