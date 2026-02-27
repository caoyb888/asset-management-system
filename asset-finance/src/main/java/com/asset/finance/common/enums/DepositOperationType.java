package com.asset.finance.common.enums;
import lombok.Getter;
@Getter
public enum DepositOperationType {
    PAY_IN(1, "缴纳"), DEDUCT(2, "冲抵应收"), REFUND(3, "退款"), FORFEIT(4, "罚没");
    private final int code; private final String desc;
    DepositOperationType(int code, String desc) { this.code = code; this.desc = desc; }
}
