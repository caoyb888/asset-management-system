package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商铺分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShopQuery extends PageQuery {

    /** 所属项目ID */
    private Long projectId;

    /** 所属楼栋ID */
    private Long buildingId;

    /** 所在楼层ID */
    private Long floorId;

    /** 铺位号（模糊） */
    private String shopCode;

    /** 商铺状态：0空置 1在租 2自用 3预留 */
    private Integer shopStatus;

    /** 商铺类型：1临街 2内铺 3专柜 */
    private Integer shopType;

    /** 签约业态（模糊） */
    private String signedFormat;
}
