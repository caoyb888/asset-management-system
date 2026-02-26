package com.asset.operation.common.enums;

import lombok.Getter;

/** 合同解约类型枚举 */
@Getter
public enum TerminationType {
    NATURAL(1, "到期自然终止"),
    EARLY(2, "提前解约"),
    RENEWAL(3, "重签新合同");

    private final int code;
    private final String desc;

    TerminationType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TerminationType of(int code) {
        for (TerminationType v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
