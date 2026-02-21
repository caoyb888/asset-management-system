package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 楼栋分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BuildingQuery extends PageQuery {

    /** 所属项目ID（必填） */
    private Long projectId;

    /** 楼栋名称（模糊） */
    private String buildingName;

    /** 楼栋编码（模糊） */
    private String buildingCode;

    /** 状态：0停用 1启用 */
    private Integer status;
}
