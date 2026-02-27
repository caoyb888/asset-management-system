package com.asset.finance.common.enums;
import lombok.Getter;
@Getter
public enum WriteOffType {
    NORMAL(1, "正常核销"), NEGATIVE(2, "负数核销"), BALANCE(3, "余额处理");
    private final int code; private final String desc;
    WriteOffType(int code, String desc) { this.code = code; this.desc = desc; }
}
