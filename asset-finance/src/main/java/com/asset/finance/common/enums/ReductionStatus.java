package com.asset.finance.common.enums;
import lombok.Getter;
@Getter
public enum ReductionStatus {
    DRAFT(0, "草稿"), APPROVING(1, "审批中"), EFFECTIVE(2, "已生效"), REJECTED(3, "已驳回");
    private final int code; private final String desc;
    ReductionStatus(int code, String desc) { this.code = code; this.desc = desc; }
    public static ReductionStatus of(int code) { for (ReductionStatus v : values()) if (v.code == code) return v; return null; }
}
