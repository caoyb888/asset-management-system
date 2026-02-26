package com.asset.operation.common.enums;

import lombok.Getter;

/** 预警发送渠道枚举 */
@Getter
public enum AlertChannel {
    SITE(1, "站内信"),
    EMAIL(2, "邮件"),
    SMS(3, "短信");

    private final int code;
    private final String desc;

    AlertChannel(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AlertChannel of(int code) {
        for (AlertChannel v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
