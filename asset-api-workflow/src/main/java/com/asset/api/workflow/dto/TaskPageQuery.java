package com.asset.api.workflow.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 待办/已办分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskPageQuery extends PageQuery {

    /** 业务类型过滤 */
    private String businessType;

    /** 审批标题模糊搜索 */
    private String title;

    /** 状态过滤 */
    private Integer status;

    /** 所属项目 ID */
    private Long projectId;
}
