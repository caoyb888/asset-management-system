package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 意向协议状态枚举
 * 对应数据字典 §4.2 inv_intention.status
 */
@Getter
@AllArgsConstructor
public enum IntentionStatus {
    DRAFT(0, "草稿"),
    APPROVING(1, "审批中"),
    APPROVED(2, "审批通过"),
    REJECTED(3, "驳回"),
    CONVERTED(4, "已转合同"),
    DELETED(5, "已删除");

    private final int code;
    private final String desc;

    public static IntentionStatus of(int code) {
        for (IntentionStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
