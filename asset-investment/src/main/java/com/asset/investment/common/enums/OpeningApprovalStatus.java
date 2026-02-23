package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 开业审批状态枚举 */
@Getter
@AllArgsConstructor
public enum OpeningApprovalStatus {
    PENDING(0, "待提交"),
    APPROVING(1, "审批中"),
    APPROVED(2, "通过"),
    REJECTED(3, "驳回");

    private final int code;
    private final String desc;

    public static OpeningApprovalStatus of(int code) {
        for (OpeningApprovalStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
