package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收费方式枚举
 * 对应数据字典 §4.3 charge_type
 */
@Getter
@AllArgsConstructor
public enum ChargeType {
    FIXED(1, "固定租金"),
    FIXED_COMMISSION(2, "固定提成"),
    STEP_COMMISSION(3, "阶梯提成"),
    HIGHER_OF(4, "两者取高"),
    ONE_TIME(5, "一次性收费");

    private final int code;
    private final String desc;

    public static ChargeType of(int code) {
        for (ChargeType v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
