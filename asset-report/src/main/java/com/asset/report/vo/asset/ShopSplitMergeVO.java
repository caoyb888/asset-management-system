package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商铺拆分合并报表 VO（P1）
 * <p>
 * 展示商铺拆分/合并操作记录及面积变动情况，
 * 数据来源：biz_shop（parent_shop_id 关联）。
 * </p>
 */
@Data
@Accessors(chain = true)
public class ShopSplitMergeVO {

    /** 商铺ID */
    private Long shopId;

    /** 商铺编码 */
    private String shopCode;

    /** 项目ID */
    private Long projectId;

    /** 楼栋ID */
    private Long buildingId;

    /** 楼层ID */
    private Long floorId;

    /**
     * 操作类型
     * SPLIT：当前商铺是由父商铺拆分而来
     * MERGE：当前商铺由多个子商铺合并而来
     * ORIGINAL：未经过拆分/合并的原始商铺
     */
    private String operationType;

    /** 操作日期（商铺创建日期） */
    private LocalDate operationDate;

    /** 父商铺ID（拆分来源，SPLIT 类型时有值） */
    private Long parentShopId;

    /** 父商铺编码 */
    private String parentShopCode;

    /** 当前商铺租赁面积（㎡） */
    private BigDecimal rentArea;

    /** 当前商铺实际面积（㎡） */
    private BigDecimal actualArea;

    /** 商铺状态（0:空置 1:出租中 2:装修 3:自用） */
    private Integer shopStatus;

    /** 计划业态 */
    private String planFormat;
}
