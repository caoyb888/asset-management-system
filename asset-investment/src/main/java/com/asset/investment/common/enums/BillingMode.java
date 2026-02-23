package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账期模式枚举
 * 对应数据字典 §4.3 billing_mode
 */
@Getter
@AllArgsConstructor
public enum BillingMode {
    PREPAY(1, "预付"),
    CURRENT(2, "当期"),
    POSTPAY(3, "后付");

    private final int code;
    private final String desc;

    public static BillingMode of(int code) {
        for (BillingMode v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
