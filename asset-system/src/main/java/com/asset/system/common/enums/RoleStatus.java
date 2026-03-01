package com.asset.system.common.enums;
import lombok.Getter;
/** 角色状态 */
@Getter
public enum RoleStatus {
    DISABLED(0, "停用"), NORMAL(1, "正常");
    private final int code; private final String desc;
    RoleStatus(int code, String desc) { this.code = code; this.desc = desc; }
}
