package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.ErrorCode;
import com.babytracker.constants.FamilyEnums;
import com.babytracker.dto.BabyView;
import com.babytracker.entity.Baby;
import com.babytracker.entity.BabyMember;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.BabyMapper;
import com.babytracker.mapper.BabyMemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BabyService {
    private final BabyMapper babyMapper;
    private final BabyMemberMapper memberMapper;
    private final FamilyService familyService;

    public BabyService(BabyMapper babyMapper, BabyMemberMapper memberMapper, FamilyService familyService) {
        this.babyMapper = babyMapper;
        this.memberMapper = memberMapper;
        this.familyService = familyService;
    }

    /** 创建宝宝档案，创建者自动成为 OWNER 成员。 */
    @Transactional
    public Baby create(Baby baby, Long creatorId) {
        if (baby.getName() == null || baby.getName().isBlank() || baby.getBirthday() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "宝宝姓名与出生日期不能为空");
        }
        baby.setId(null);
        baby.setCreatedBy(creatorId);
        babyMapper.insert(baby);
        BabyMember owner = new BabyMember();
        owner.setBabyId(baby.getId());
        owner.setUserId(creatorId);
        owner.setRole(FamilyEnums.ROLE_OWNER);
        memberMapper.insert(owner);
        return baby;
    }

    /** 当前用户可见（已加入）的宝宝列表，附带自己的角色。 */
    public List<BabyView> listMine(Long userId) {
        List<BabyMember> memberships = memberMapper.selectList(
                new QueryWrapper<BabyMember>().eq("user_id", userId));
        if (memberships.isEmpty()) return List.of();
        Map<Long, BabyMember> byBabyId = memberships.stream()
                .collect(Collectors.toMap(BabyMember::getBabyId, Function.identity()));
        return babyMapper.selectBatchIds(byBabyId.keySet()).stream()
                .map(baby -> BabyView.of(baby, byBabyId.get(baby.getId()).getRole()))
                .collect(Collectors.toList());
    }

    /** 查看档案：任何家庭成员可读；待认领的旧宝宝对所有登录用户只读开放。 */
    public Baby getForMember(Long babyId, Long userId) {
        familyService.requireRole(babyId, userId, FamilyEnums.ROLE_VIEW);
        Baby baby = babyMapper.selectById(babyId);
        if (baby == null) throw new BizException(ErrorCode.NOT_FOUND, "宝宝档案不存在");
        return baby;
    }

    /** 没有创建者的旧宝宝列表（升级前的数据），等待认领。 */
    public List<Baby> listAdoptable() {
        return babyMapper.selectOwnerless();
    }

    /** 修改档案：仅管理者。 */
    public Baby update(Long babyId, Baby changes, Long userId) {
        familyService.requireRole(babyId, userId, FamilyEnums.ROLE_MANAGE);
        Baby baby = babyMapper.selectById(babyId);
        if (baby == null) throw new BizException(ErrorCode.NOT_FOUND, "宝宝档案不存在");
        if (changes.getName() != null && !changes.getName().isBlank()) baby.setName(changes.getName());
        if (changes.getBirthday() != null) baby.setBirthday(changes.getBirthday());
        if (changes.getBloodType() != null) baby.setBloodType(changes.getBloodType());
        if (changes.getInitialHeight() != null) baby.setInitialHeight(changes.getInitialHeight());
        if (changes.getInitialWeight() != null) baby.setInitialWeight(changes.getInitialWeight());
        babyMapper.updateById(baby);
        return baby;
    }
}
