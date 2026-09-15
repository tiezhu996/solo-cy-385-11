package com.babytracker.constants;

/**
 * 家庭协作常量：成员角色与邀请码状态。
 * 角色权限等级：VIEW(查看) < RECORD(记录) < MANAGE(管理) < OWNER(创建者)。
 */
public final class FamilyEnums {
    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MANAGE = "MANAGE";
    public static final String ROLE_RECORD = "RECORD";
    public static final String ROLE_VIEW = "VIEW";

    public static final String INVITE_ACTIVE = "ACTIVE";
    public static final String INVITE_REVOKED = "REVOKED";
    public static final String INVITE_CLAIMED = "CLAIMED";

    private FamilyEnums() {}

    /** 角色权限等级，数值越大权限越高；未知角色按 0 处理。 */
    public static int rank(String role) {
        if (ROLE_OWNER.equals(role)) return 4;
        if (ROLE_MANAGE.equals(role)) return 3;
        if (ROLE_RECORD.equals(role)) return 2;
        if (ROLE_VIEW.equals(role)) return 1;
        return 0;
    }

    /** 是否具备管理能力（创建者与管理者）。 */
    public static boolean canManage(String role) {
        return rank(role) >= rank(ROLE_MANAGE);
    }

    /** 邀请码与角色调整允许使用的角色（创建者角色不可授予）。 */
    public static boolean isGrantableRole(String role) {
        return ROLE_MANAGE.equals(role) || ROLE_RECORD.equals(role) || ROLE_VIEW.equals(role);
    }

    /** 角色中文名，用于提示与展示。 */
    public static String roleName(String role) {
        if (ROLE_OWNER.equals(role)) return "创建者";
        if (ROLE_MANAGE.equals(role)) return "管理";
        if (ROLE_RECORD.equals(role)) return "记录";
        if (ROLE_VIEW.equals(role)) return "查看";
        return "未知";
    }
}
