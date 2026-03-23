package com.asset.api.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批动作枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovalAction {

    APPROVE(1, "通过"),
    REJECT(2, "驳回"),
    REASSIGN(3, "转办"),
    ADD_SIGN(4, "加签"),
    REVOKE(5, "撤回");

    private final int code;
    private final String label;

    public static ApprovalAction fromCode(int code) {
        for (ApprovalAction a : values()) {
            if (a.code == code) return a;
        }
        throw new IllegalArgumentException("未知审批动作: " + code);
    }
}
