package com.asset.api.workflow.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程实例分页查询（管理员用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessPageQuery extends PageQuery {

    /** 业务类型过滤 */
    private String businessType;

    /** 审批标题模糊搜索 */
    private String title;

    /** 状态过滤 */
    private Integer status;

    /** 发起人 ID */
    private Long initiatorId;

    /** 所属项目 ID */
    private Long projectId;
}
