package com.asset.operation.common.enums;

import lombok.Getter;

/** 合同变更状态枚举 */
@Getter
public enum ChangeStatus {
    DRAFT(0, "草稿"),
    APPROVING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回");

    private final int code;
    private final String desc;

    ChangeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ChangeStatus of(int code) {
        for (ChangeStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
