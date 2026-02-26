package com.asset.operation.common.enums;

import lombok.Getter;

/** 应收计划状态枚举 */
@Getter
public enum ReceivablePlanStatus {
    PENDING(0, "待收"),
    PARTIAL(1, "部分收款"),
    COLLECTED(2, "已收"),
    VOIDED(3, "已作废");

    private final int code;
    private final String desc;

    ReceivablePlanStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReceivablePlanStatus of(int code) {
        for (ReceivablePlanStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
