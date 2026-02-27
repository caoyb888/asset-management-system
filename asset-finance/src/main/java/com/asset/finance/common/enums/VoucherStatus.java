package com.asset.finance.common.enums;
import lombok.Getter;
@Getter
public enum VoucherStatus {
    DRAFT(0, "草稿"), AUDITED(1, "已审核"), POSTED(2, "已过账");
    private final int code; private final String desc;
    VoucherStatus(int code, String desc) { this.code = code; this.desc = desc; }
}
