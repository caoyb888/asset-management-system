package com.asset.api.workflow.dto;

import lombok.Data;

/**
 * 统一审批回调 DTO — workflow 回调业务模块时使用
 */
@Data
public class ApprovalCallbackDTO {

    /** 流程实例 ID */
    private String processInstanceId;

    /** 业务类型（ApprovalBusinessType 枚举值） */
    private String businessType;

    /** 业务单据 ID */
    private Long businessId;

    /** 审批结果：2=通过, 3=驳回 */
    private Integer result;

    /** 最终审批意见 */
    private String comment;

    /** 审批人 ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;
}
