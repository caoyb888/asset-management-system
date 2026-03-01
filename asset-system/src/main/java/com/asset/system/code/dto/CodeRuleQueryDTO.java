package com.asset.system.code.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 编码规则分页查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeRuleQueryDTO extends PageQuery {

    /** 规则名称模糊查询 */
    private String ruleName;

    /** 规则标识模糊查询 */
    private String ruleKey;

    /** 状态筛选 */
    private Integer status;
}
