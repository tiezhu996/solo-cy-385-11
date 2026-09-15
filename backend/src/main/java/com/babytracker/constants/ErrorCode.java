package com.babytracker.constants;

public final class ErrorCode {
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NICKNAME_TAKEN = "NICKNAME_TAKEN";
    public static final String PASSWORD_NOT_SET = "PASSWORD_NOT_SET";
    public static final String PASSWORD_ALREADY_SET = "PASSWORD_ALREADY_SET";
    public static final String ALREADY_MEMBER = "ALREADY_MEMBER";
    public static final String INVITE_INVALID = "INVITE_INVALID";
    public static final String INVITE_EXPIRED = "INVITE_EXPIRED";
    public static final String INVITE_REVOKED = "INVITE_REVOKED";
    public static final String INVITE_CLAIMED = "INVITE_CLAIMED";
    public static final String CREATOR_FORBIDDEN = "CREATOR_FORBIDDEN";
    public static final String LAST_MANAGER_REQUIRED = "LAST_MANAGER_REQUIRED";
    public static final String BABY_ALREADY_OWNED = "BABY_ALREADY_OWNED";
    private ErrorCode() {}
}
