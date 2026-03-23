package com.asset.api.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批业务类型枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovalBusinessType {

    INV_INTENTION("意向协议审批", "asset-investment"),
    INV_OPENING("开业审批", "asset-investment"),
    INV_RENT_DECOMP("租金分解审批", "asset-investment"),
    OPR_CONTRACT_CHANGE("合同变更审批", "asset-operation"),
    OPR_TERMINATION("合同解约审批", "asset-operation"),
    FIN_WRITE_OFF("核销审批", "asset-finance"),
    FIN_DEDUCTION("减免审批", "asset-finance"),
    FIN_ADJUSTMENT("调整审批", "asset-finance");

    /** 业务描述 */
    private final String label;
    /** 回调目标服务名 */
    private final String callbackService;
}
