package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 商铺类别枚举（用于租决政策分类指标） */
@Getter
@AllArgsConstructor
public enum ShopCategory {
    ANCHOR(1, "主力店"),
    SUB_ANCHOR(2, "次主力店"),
    GENERAL(3, "一般商铺");

    private final int code;
    private final String desc;

    public static ShopCategory of(int code) {
        for (ShopCategory v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
