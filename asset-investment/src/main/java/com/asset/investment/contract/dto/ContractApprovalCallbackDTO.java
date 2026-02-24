package com.asset.investment.contract.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合同审批回调请求体
 * 由审批引擎回调或前端 Mock 审批时使用
 */
@Data
public class ContractApprovalCallbackDTO {

    /** 是否通过（true=通过 → EFFECTIVE；false=驳回 → DRAFT） */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 审批引擎返回的流程实例ID（可选） */
    private String approvalId;

    /** 审批意见/驳回原因 */
    private String comment;
}
