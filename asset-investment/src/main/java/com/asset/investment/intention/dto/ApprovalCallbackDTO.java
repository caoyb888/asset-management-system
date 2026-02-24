package com.asset.investment.intention.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批回调请求体（任务 4.3）
 * 由审批引擎回调或前端 Mock 审批时使用
 */
@Data
public class ApprovalCallbackDTO {

    /** 是否通过（true=通过 → status=2；false=驳回 → status=3） */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 审批引擎返回的流程实例ID（可选，用于追溯） */
    private String approvalId;

    /** 审批意见/驳回原因 */
    private String comment;
}
