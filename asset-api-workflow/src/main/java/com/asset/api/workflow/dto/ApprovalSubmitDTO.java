package com.asset.api.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 统一审批提交 DTO
 */
@Data
public class ApprovalSubmitDTO {

    /** 业务类型（ApprovalBusinessType 枚举值） */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /** 业务单据 ID */
    @NotNull(message = "业务单据ID不能为空")
    private Long businessId;

    /** 审批标题 */
    @NotBlank(message = "审批标题不能为空")
    private String title;

    /** 发起人用户 ID（可从 SecurityContext 获取） */
    private Long initiatorId;

    /** 发起人姓名（冗余） */
    private String initiatorName;

    /** 所属项目 ID（数据权限过滤） */
    private Long projectId;

    /** 优先级：0=普通 1=紧急 2=加急 */
    private Integer priority;

    /** 扩展变量（金额、合同号等，供流程条件判断） */
    private Map<String, Object> variables;
}
