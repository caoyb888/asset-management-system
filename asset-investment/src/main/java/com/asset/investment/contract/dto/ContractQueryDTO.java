package com.asset.investment.contract.dto;

import lombok.Data;

/**
 * 招商合同分页查询条件
 */
@Data
public class ContractQueryDTO {

    private int pageNum = 1;
    private int pageSize = 20;

    /** 所属项目ID */
    private Long projectId;

    /** 合同状态：0草稿/1审批中/2生效/3到期/4终止 */
    private Integer status;

    /** 关键词（合同名称/编号模糊搜索） */
    private String keyword;

    /** 商家ID */
    private Long merchantId;
}
