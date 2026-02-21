package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 楼层分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FloorQuery extends PageQuery {

    /** 所属项目ID */
    private Long projectId;

    /** 所属楼栋ID（必填） */
    private Long buildingId;

    /** 楼层名称（模糊） */
    private String floorName;

    /** 楼层编码（模糊） */
    private String floorCode;

    /** 状态：0停用 1启用 */
    private Integer status;
}
