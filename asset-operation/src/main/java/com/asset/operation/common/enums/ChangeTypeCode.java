package com.asset.operation.common.enums;

import lombok.Getter;

/** 合同变更类型编码枚举（对应 opr_contract_change_type.change_type_code） */
@Getter
public enum ChangeTypeCode {
    RENT("RENT", "租金变更"),
    BRAND("BRAND", "品牌变更"),
    TENANT("TENANT", "租户主体变更"),
    FEE("FEE", "租费单价变更"),
    CLAUSE("CLAUSE", "合同条款变更"),
    TERM("TERM", "租期变更"),
    AREA("AREA", "面积变更"),
    COMPANY("COMPANY", "公司名称变更");

    private final String code;
    private final String desc;

    ChangeTypeCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ChangeTypeCode of(String code) {
        for (ChangeTypeCode v : values()) {
            if (v.code.equals(code)) return v;
        }
        return null;
    }
}
