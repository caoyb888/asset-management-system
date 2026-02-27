package com.asset.finance.common.enums;
import lombok.Getter;
@Getter
public enum ReceiptStatus {
    PENDING(0, "待核销"), PARTIAL(1, "部分核销"), DONE(2, "已核销"), VOID(3, "已作废");
    private final int code; private final String desc;
    ReceiptStatus(int code, String desc) { this.code = code; this.desc = desc; }
    public static ReceiptStatus of(int code) { for (ReceiptStatus v : values()) if (v.code == code) return v; return null; }
}
