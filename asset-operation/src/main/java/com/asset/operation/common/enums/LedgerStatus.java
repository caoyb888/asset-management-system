package com.asset.operation.common.enums;

import lombok.Getter;

/** 合同台账状态枚举 */
@Getter
public enum LedgerStatus {
    ONGOING(0, "进行中"),
    COMPLETED(1, "已完成"),
    TERMINATED(2, "已解约");

    private final int code;
    private final String desc;

    LedgerStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static LedgerStatus of(int code) {
        for (LedgerStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
