package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付周期枚举
 * 对应数据字典 §4.3 payment_cycle
 */
@Getter
@AllArgsConstructor
public enum PaymentCycle {
    MONTHLY(1, "月付"),
    BIMONTHLY(2, "两月付"),
    QUARTERLY(3, "季付"),
    FOUR_MONTH(4, "四月付"),
    SEMIANNUAL(5, "半年付"),
    ANNUAL(6, "年付");

    private final int code;
    private final String desc;

    /** 返回该周期对应的月数 */
    public int getMonths() {
        return switch (this.code) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            case 5 -> 6;
            default -> 12;
        };
    }

    public static PaymentCycle of(int code) {
        for (PaymentCycle v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
