package com.asset.api.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProcessStatus {

    PENDING(0, "待审批"),
    IN_PROGRESS(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回"),
    REVOKED(4, "已撤回"),
    CANCELLED(5, "已作废");

    private final int code;
    private final String label;

    public static ProcessStatus fromCode(int code) {
        for (ProcessStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知流程状态: " + code);
    }
}
