package com.asset.system.common.enums;
import lombok.Getter;
/** 用户状态 */
@Getter
public enum UserStatus {
    DISABLED(0, "停用"), NORMAL(1, "正常");
    private final int code; private final String desc;
    UserStatus(int code, String desc) { this.code = code; this.desc = desc; }
    public static UserStatus of(int code) { for (UserStatus v : values()) if (v.code == code) return v; return null; }
}
