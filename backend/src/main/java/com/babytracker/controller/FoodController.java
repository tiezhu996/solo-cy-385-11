package com.babytracker.controller;

import com.babytracker.constants.FamilyEnums;
import com.babytracker.entity.FoodRecipe;
import com.babytracker.service.FamilyService;
import com.babytracker.service.FoodService;
import com.babytracker.utils.AuthContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 辅食推荐：家庭成员（VIEW 及以上）可按月龄与过敏原查询，每次请求实时校验成员角色。 */
@RestController
@RequestMapping("/api/babies/{babyId}/foods")
public class FoodController {
    private final FoodService service;
    private final FamilyService familyService;

    public FoodController(FoodService service, FamilyService familyService) {
        this.service = service;
        this.familyService = familyService;
    }

    @GetMapping("/recommend")
    public List<FoodRecipe> recommend(@PathVariable("babyId") Long babyId,
                                      @RequestParam Integer monthAge,
                                      @RequestParam(required = false) String allergen) {
        familyService.requireRole(babyId, AuthContext.requireUserId(), FamilyEnums.ROLE_VIEW);
        return service.recommend(monthAge, allergen);
    }
}
