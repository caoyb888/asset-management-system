package com.asset.operation.common.enums;

import lombok.Getter;

/** 合同解约状态枚举 */
@Getter
public enum TerminationStatus {
    DRAFT(0, "草稿"),
    APPROVING(1, "审批中"),
    EFFECTIVE(2, "已生效"),
    REJECTED(3, "已驳回");

    private final int code;
    private final String desc;

    TerminationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TerminationStatus of(int code) {
        for (TerminationStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
