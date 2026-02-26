package com.asset.operation.common.enums;

import lombok.Getter;

/** 预警类型枚举 */
@Getter
public enum AlertType {
    CONTRACT_EXPIRY(1, "合同到期预警"),
    RECEIVABLE_OVERDUE(2, "应收到期预警");

    private final int code;
    private final String desc;

    AlertType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AlertType of(int code) {
        for (AlertType v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
