package com.asset.finance.common.enums;
import lombok.Getter;
@Getter
public enum ReceivableStatus {
    PENDING(0, "待收"), PARTIAL(1, "部分收款"), RECEIVED(2, "已收"), VOID(3, "已作废"), REDUCED(4, "已减免");
    private final int code; private final String desc;
    ReceivableStatus(int code, String desc) { this.code = code; this.desc = desc; }
    public static ReceivableStatus of(int code) { for (ReceivableStatus v : values()) if (v.code == code) return v; return null; }
}
