package com.babytracker.controller;

import com.babytracker.dto.ChangeRoleRequest;
import com.babytracker.dto.ClaimInviteRequest;
import com.babytracker.dto.CreateInviteRequest;
import com.babytracker.entity.BabyInvite;
import com.babytracker.entity.BabyMember;
import com.babytracker.service.FamilyService;
import com.babytracker.utils.AuthContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 家庭协作：邀请码与成员权限管理。 */
@RestController
@RequestMapping("/api")
public class FamilyController {
    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) { this.familyService = familyService; }

    // ---------- 邀请码 ----------

    @PostMapping("/babies/{babyId}/invites")
    public BabyInvite createInvite(@PathVariable("babyId") Long babyId, @RequestBody CreateInviteRequest request) {
        return familyService.createInvite(babyId, AuthContext.requireUserId(),
                request.getRole(), request.getTtlMinutes());
    }

    @GetMapping("/babies/{babyId}/invites")
    public List<BabyInvite> listInvites(@PathVariable("babyId") Long babyId) {
        return familyService.listInvites(babyId, AuthContext.requireUserId());
    }

    @PostMapping("/babies/{babyId}/invites/{inviteId}/revoke")
    public Map<String, Object> revokeInvite(@PathVariable("babyId") Long babyId, @PathVariable("inviteId") Long inviteId) {
        familyService.revokeInvite(babyId, inviteId, AuthContext.requireUserId());
        return Map.of("success", true);
    }

    @PostMapping("/invites/claim")
    public BabyMember claimInvite(@RequestBody ClaimInviteRequest request) {
        return familyService.claimInvite(AuthContext.requireUserId(), request.getCode());
    }

    // ---------- 成员与角色 ----------

    @GetMapping("/babies/{babyId}/members")
    public List<BabyMember> listMembers(@PathVariable("babyId") Long babyId) {
        return familyService.listMembers(babyId, AuthContext.requireUserId());
    }

    @PutMapping("/babies/{babyId}/members/{userId}")
    public BabyMember changeRole(@PathVariable("babyId") Long babyId, @PathVariable("userId") Long userId,
                                 @RequestBody ChangeRoleRequest request) {
        return familyService.changeRole(babyId, AuthContext.requireUserId(), userId, request.getRole());
    }

    @DeleteMapping("/babies/{babyId}/members/{userId}")
    public Map<String, Object> removeMember(@PathVariable("babyId") Long babyId, @PathVariable("userId") Long userId) {
        familyService.removeMember(babyId, AuthContext.requireUserId(), userId);
        return Map.of("success", true);
    }

    @PostMapping("/babies/{babyId}/leave")
    public Map<String, Object> leave(@PathVariable("babyId") Long babyId) {
        familyService.leave(babyId, AuthContext.requireUserId());
        return Map.of("success", true);
    }
}
