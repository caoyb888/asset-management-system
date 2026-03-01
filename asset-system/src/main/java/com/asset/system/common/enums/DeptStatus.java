package com.asset.system.common.enums;
import lombok.Getter;
/** 部门状态 */
@Getter
public enum DeptStatus {
    DISABLED(0, "停用"), NORMAL(1, "正常");
    private final int code; private final String desc;
    DeptStatus(int code, String desc) { this.code = code; this.desc = desc; }
}
