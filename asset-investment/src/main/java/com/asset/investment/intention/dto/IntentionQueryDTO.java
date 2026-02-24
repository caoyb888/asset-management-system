package com.asset.investment.intention.dto;

import lombok.Data;

/**
 * 意向协议分页查询条件
 */
@Data
public class IntentionQueryDTO {

    /** 页码（从1开始） */
    private int pageNum = 1;

    /** 每页条数 */
    private int pageSize = 20;

    /** 项目ID */
    private Long projectId;

    /** 状态(0草稿/1审批中/2审批通过/3驳回/4已转合同) */
    private Integer status;

    /** 商家ID */
    private Long merchantId;

    /** 品牌ID */
    private Long brandId;

    /** 楼栋ID（联表 inv_intention_shop 筛选） */
    private Long buildingId;

    /** 楼层ID（联表筛选） */
    private Long floorId;

    /** 商铺ID（联表筛选） */
    private Long shopId;

    /** 业态（联表筛选） */
    private String formatType;

    /** 关键词（意向名称/编号模糊搜索） */
    private String keyword;
}
