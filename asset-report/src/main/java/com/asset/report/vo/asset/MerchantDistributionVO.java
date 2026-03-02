package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 商家分布报表 VO（P1）
 * <p>
 * 按楼栋/楼层/业态维度展示已签约商家的分布情况，
 * 基于 rpt_asset_daily 楼层粒度数据。
 * </p>
 */
@Data
@Accessors(chain = true)
public class MerchantDistributionVO {

    /** 项目ID */
    private Long projectId;

    /** 楼栋ID */
    private Long buildingId;

    /** 楼层ID */
    private Long floorId;

    /** 业态类型 */
    private String formatType;

    /** 已签约（已租）商铺数（即入驻商家数） */
    private Integer merchantCount;

    /** 商铺总数 */
    private Integer totalShops;

    /** 出租率（%） */
    private BigDecimal rentalRate;

    /** 已租面积（㎡） */
    private BigDecimal rentedArea;

    /** 总面积（㎡） */
    private BigDecimal totalArea;
}
