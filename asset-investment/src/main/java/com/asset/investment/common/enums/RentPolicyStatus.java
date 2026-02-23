package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 租决政策/租金分解状态枚举 */
@Getter
@AllArgsConstructor
public enum RentPolicyStatus {
    DRAFT(0, "草稿"),
    APPROVING(1, "审批中"),
    APPROVED(2, "通过"),
    REJECTED(3, "驳回");

    private final int code;
    private final String desc;

    public static RentPolicyStatus of(int code) {
        for (RentPolicyStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
