package com.asset.investment.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 租赁合同状态枚举
 * 对应数据字典 §4.2 inv_lease_contract.status
 */
@Getter
@AllArgsConstructor
public enum ContractStatus {
    DRAFT(0, "草稿"),
    APPROVING(1, "审批中"),
    EFFECTIVE(2, "生效"),
    EXPIRED(3, "到期"),
    TERMINATED(4, "终止"),
    DELETED(5, "已删除");

    private final int code;
    private final String desc;

    public static ContractStatus of(int code) {
        for (ContractStatus v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
