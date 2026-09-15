package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.ErrorCode;
import com.babytracker.constants.FamilyEnums;
import com.babytracker.entity.AppUser;
import com.babytracker.entity.Baby;
import com.babytracker.entity.BabyInvite;
import com.babytracker.entity.BabyMember;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.AppUserMapper;
import com.babytracker.mapper.BabyInviteMapper;
import com.babytracker.mapper.BabyMapper;
import com.babytracker.mapper.BabyMemberMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 家庭协作核心逻辑：邀请码、成员角色与权限校验。
 *
 * 并发语义：
 * - 领取邀请码通过单条原子 UPDATE 判定胜负，同一邀请码只会被一个用户领取成功；
 * - 角色调整 / 移除成员 / 退出家庭先对 baby 行加排他锁（SELECT ... FOR UPDATE），
 *   并在 READ_COMMITTED 下于持锁后复查操作者权限与目标状态，
 *   同一宝宝的成员变更被串行化，保证只留下一个确定结果；
 * - 所有数据接口每次请求实时查询成员表，角色变更与移除立即生效。
 */
@Service
public class FamilyService {
    private static final long DEFAULT_INVITE_TTL_MINUTES = 72 * 60;
    private static final long MAX_INVITE_TTL_MINUTES = 7L * 24 * 60;

    private final BabyMapper babyMapper;
    private final BabyMemberMapper memberMapper;
    private final BabyInviteMapper inviteMapper;
    private final AppUserMapper userMapper;

    public FamilyService(BabyMapper babyMapper, BabyMemberMapper memberMapper,
                         BabyInviteMapper inviteMapper, AppUserMapper userMapper) {
        this.babyMapper = babyMapper;
        this.memberMapper = memberMapper;
        this.inviteMapper = inviteMapper;
        this.userMapper = userMapper;
    }

    // ---------- 权限校验 ----------

    /** 校验当前用户是该宝宝家庭成员，返回成员记录；否则 403。 */
    public BabyMember requireMember(Long babyId, Long userId) {
        BabyMember member = findMember(babyId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "不是该宝宝的家庭成员，无权访问");
        }
        return member;
    }

    /**
     * 校验当前用户角色达到 minRole（VIEW/RECORD/MANAGE）。
     * 升级前创建、尚无创建者的旧宝宝（ownerless）对所有登录用户只读开放，
     * 便于查看旧档案并认领；写入与管理操作仍需先认领成为成员。
     */
    public BabyMember requireRole(Long babyId, Long userId, String minRole) {
        BabyMember member = findMember(babyId, userId);
        if (member == null) {
            if (FamilyEnums.rank(minRole) <= FamilyEnums.rank(FamilyEnums.ROLE_VIEW) && isOwnerless(babyId)) {
                return null;
            }
            throw new BizException(ErrorCode.FORBIDDEN, "不是该宝宝的家庭成员，无权访问");
        }
        if (FamilyEnums.rank(member.getRole()) < FamilyEnums.rank(minRole)) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    "当前角色权限不足，需要" + FamilyEnums.roleName(minRole) + "权限");
        }
        return member;
    }

    /** 该宝宝是否没有任何 OWNER 成员（待认领的旧档案）。 */
    public boolean isOwnerless(Long babyId) {
        return memberMapper.selectCount(new QueryWrapper<BabyMember>()
                .eq("baby_id", babyId).eq("role", FamilyEnums.ROLE_OWNER)) == 0;
    }

    public BabyMember findMember(Long babyId, Long userId) {
        return memberMapper.selectOne(new QueryWrapper<BabyMember>()
                .eq("baby_id", babyId).eq("user_id", userId));
    }

    // ---------- 旧档案认领 ----------

    /**
     * 认领没有创建者的旧宝宝：认领人成为 OWNER，created_by 一并补齐。
     * 通过宝宝行锁串行化，并发认领只会有一个确定结果。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BabyMember adopt(Long babyId, Long userId) {
        lockBaby(babyId);
        if (findMember(babyId, userId) != null) {
            throw new BizException(ErrorCode.ALREADY_MEMBER, "你已经是该宝宝的家庭成员");
        }
        if (!isOwnerless(babyId)) {
            throw new BizException(ErrorCode.BABY_ALREADY_OWNED, "该宝宝已有创建者，请通过邀请码加入");
        }
        Baby baby = babyMapper.selectById(babyId);
        if (baby.getCreatedBy() != null && !baby.getCreatedBy().equals(userId)) {
            throw new BizException(ErrorCode.BABY_ALREADY_OWNED, "该宝宝已有创建者，请通过邀请码加入");
        }
        if (baby.getCreatedBy() == null) {
            baby.setCreatedBy(userId);
            babyMapper.updateById(baby);
        }
        BabyMember member = new BabyMember();
        member.setBabyId(babyId);
        member.setUserId(userId);
        member.setRole(FamilyEnums.ROLE_OWNER);
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.ALREADY_MEMBER, "你已经是该宝宝的家庭成员");
        }
        return member;
    }

    // ---------- 邀请码 ----------

    /** 生成一次性邀请码，仅管理者可操作。 */
    public BabyInvite createInvite(Long babyId, Long operatorId, String role, Long ttlMinutes) {
        requireRole(babyId, operatorId, FamilyEnums.ROLE_MANAGE);
        requireBaby(babyId);
        if (!FamilyEnums.isGrantableRole(role)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "角色必须是 VIEW、RECORD 或 MANAGE");
        }
        long ttl = ttlMinutes == null ? DEFAULT_INVITE_TTL_MINUTES : ttlMinutes;
        if (ttl <= 0 || ttl > MAX_INVITE_TTL_MINUTES) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "有效期需在 1 分钟到 7 天之间");
        }
        BabyInvite invite = new BabyInvite();
        invite.setBabyId(babyId);
        invite.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        invite.setRole(role);
        invite.setStatus(FamilyEnums.INVITE_ACTIVE);
        invite.setExpiresAt(LocalDateTime.now().plusMinutes(ttl));
        invite.setCreatedBy(operatorId);
        inviteMapper.insert(invite);
        return invite;
    }

    /** 邀请码列表回读，仅管理者可见。 */
    public List<BabyInvite> listInvites(Long babyId, Long operatorId) {
        requireRole(babyId, operatorId, FamilyEnums.ROLE_MANAGE);
        return inviteMapper.selectList(new QueryWrapper<BabyInvite>()
                .eq("baby_id", babyId).orderByDesc("id"));
    }

    /** 撤销邀请码：原子更新，已领取或已撤销的邀请码不能再次操作。 */
    public void revokeInvite(Long babyId, Long inviteId, Long operatorId) {
        requireRole(babyId, operatorId, FamilyEnums.ROLE_MANAGE);
        int updated = inviteMapper.revokeIfActive(inviteId, babyId);
        if (updated == 0) {
            throw new BizException(ErrorCode.INVITE_INVALID, "邀请码不存在或已被领取/撤销");
        }
    }

    /**
     * 领取邀请码：原子 UPDATE 保证同一邀请码只生效一次；
     * 过期、已撤销、已领取的邀请码均领取失败。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BabyMember claimInvite(Long userId, String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "邀请码不能为空");
        }
        BabyInvite invite = inviteMapper.selectOne(new QueryWrapper<BabyInvite>()
                .eq("code", code.trim()));
        if (invite == null) {
            throw new BizException(ErrorCode.INVITE_INVALID, "邀请码不存在");
        }
        if (findMember(invite.getBabyId(), userId) != null) {
            throw new BizException(ErrorCode.ALREADY_MEMBER, "你已经是该宝宝的家庭成员");
        }
        int updated = inviteMapper.claimIfActive(invite.getId(), userId);
        if (updated == 0) {
            // 领取失败，读取最新已提交状态给出精确原因
            BabyInvite current = inviteMapper.selectById(invite.getId());
            if (FamilyEnums.INVITE_REVOKED.equals(current.getStatus())) {
                throw new BizException(ErrorCode.INVITE_REVOKED, "邀请码已被撤销");
            }
            if (FamilyEnums.INVITE_CLAIMED.equals(current.getStatus())) {
                throw new BizException(ErrorCode.INVITE_CLAIMED, "邀请码已被使用");
            }
            throw new BizException(ErrorCode.INVITE_EXPIRED, "邀请码已过期");
        }
        BabyMember member = new BabyMember();
        member.setBabyId(invite.getBabyId());
        member.setUserId(userId);
        member.setRole(invite.getRole());
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            // 并发下同一用户通过不同邀请码重复加入：回滚，邀请码保持 ACTIVE
            throw new BizException(ErrorCode.ALREADY_MEMBER, "你已经是该宝宝的家庭成员");
        }
        return member;
    }

    // ---------- 成员管理 ----------

    /** 成员与权限列表回读，家庭成员可见；待认领的旧宝宝也可查看（便于确认状态）。 */
    public List<BabyMember> listMembers(Long babyId, Long operatorId) {
        requireRole(babyId, operatorId, FamilyEnums.ROLE_VIEW);
        List<BabyMember> members = memberMapper.selectList(new QueryWrapper<BabyMember>()
                .eq("baby_id", babyId).orderByAsc("id"));
        Map<Long, AppUser> users = userMapper.selectBatchIds(
                        members.stream().map(BabyMember::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        members.forEach(m -> {
            AppUser user = users.get(m.getUserId());
            m.setNickname(user != null ? user.getNickname() : "未知用户");
        });
        return members;
    }

    /** 调整成员角色：仅管理者；创建者角色不可变更；不能失去最后一名管理者。 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BabyMember changeRole(Long babyId, Long operatorId, Long targetUserId, String newRole) {
        requireRole(babyId, operatorId, FamilyEnums.ROLE_MANAGE);
        if (!FamilyEnums.isGrantableRole(newRole)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "角色必须是 VIEW、RECORD 或 MANAGE");
        }
        lockBaby(babyId);
        // 持锁后复查：操作者可能在加锁期间被移除或降级
        requireRole(babyId, operatorId, FamilyEnums.ROLE_MANAGE);
        BabyMember target = requireMember(babyId, targetUserId);
        if (FamilyEnums.ROLE_OWNER.equals(target.getRole())) {
            throw new BizException(ErrorCode.CREATOR_FORBIDDEN, "不能修改创建者的角色");
        }
        if (FamilyEnums.canManage(target.getRole()) && !FamilyEnums.canManage(newRole)) {
            ensureNotLastManager(babyId);
        }
        target.setRole(newRole);
        memberMapper.updateById(target);
        return target;
    }

    /** 移除成员：仅管理者；创建者不可移除；最后一名管理者不可移除。 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeMember(Long babyId, Long operatorId, Long targetUserId) {
        requireRole(babyId, operatorId, FamilyEnums.ROLE_MANAGE);
        lockBaby(babyId);
        requireRole(babyId, operatorId, FamilyEnums.ROLE_MANAGE);
        BabyMember target = requireMember(babyId, targetUserId);
        if (FamilyEnums.ROLE_OWNER.equals(target.getRole())) {
            throw new BizException(ErrorCode.CREATOR_FORBIDDEN, "不能移除创建者");
        }
        if (FamilyEnums.canManage(target.getRole())) {
            ensureNotLastManager(babyId);
        }
        memberMapper.deleteById(target.getId());
    }

    /** 退出家庭：创建者不能退出；最后一名管理者不能退出。 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void leave(Long babyId, Long userId) {
        requireMember(babyId, userId);
        lockBaby(babyId);
        BabyMember self = requireMember(babyId, userId);
        if (FamilyEnums.ROLE_OWNER.equals(self.getRole())) {
            throw new BizException(ErrorCode.CREATOR_FORBIDDEN, "创建者不能退出家庭");
        }
        if (FamilyEnums.canManage(self.getRole())) {
            ensureNotLastManager(babyId);
        }
        memberMapper.deleteById(self.getId());
    }

    // ---------- 内部工具 ----------

    private void requireBaby(Long babyId) {
        if (babyMapper.selectById(babyId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "宝宝档案不存在");
        }
    }

    /** 对宝宝行加排他锁，串行化成员变更；宝宝不存在时抛 404。 */
    private void lockBaby(Long babyId) {
        if (babyMapper.lockById(babyId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "宝宝档案不存在");
        }
    }

    /** 在持有宝宝行锁的前提下校验：操作后家庭中至少保留一名具备管理能力的成员。 */
    private void ensureNotLastManager(Long babyId) {
        if (memberMapper.countManagers(babyId) <= 1) {
            throw new BizException(ErrorCode.LAST_MANAGER_REQUIRED, "至少保留一名管理者");
        }
    }
}
