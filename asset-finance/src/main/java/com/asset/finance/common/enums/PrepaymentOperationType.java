package com.asset.finance.common.enums;
import lombok.Getter;
@Getter
public enum PrepaymentOperationType {
    RECEIVE(1, "暂存入账"), DEDUCT(2, "抵冲应收"), REFUND(3, "退款");
    private final int code; private final String desc;
    PrepaymentOperationType(int code, String desc) { this.code = code; this.desc = desc; }
}
