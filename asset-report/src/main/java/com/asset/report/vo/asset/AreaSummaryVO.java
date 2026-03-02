package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 经营面积统计报表 VO（P1）
 * <p>
 * 按项目汇总各维度面积数据，支持同比增长率对比。
 * 数据来源：rpt_asset_daily（项目级汇总行）。
 * </p>
 */
@Data
@Accessors(chain = true)
public class AreaSummaryVO {

    /** 项目ID */
    private Long projectId;

    /** 总建筑面积（㎡） */
    private BigDecimal totalArea;

    /** 已租面积（㎡） */
    private BigDecimal rentedArea;

    /** 空置面积（㎡） */
    private BigDecimal vacantArea;

    /** 装修中面积（㎡） */
    private BigDecimal decorationArea;

    /** 出租率（%） */
    private BigDecimal rentalRate;

    /** 空置率（%） */
    private BigDecimal vacancyRate;

    /** 已租面积同比增长率（%），去年同日数据不存在时为 null */
    private BigDecimal rentedAreaYoY;

    /** 空置率同比变化（pp，百分点），如 -2.50 表示同比下降 2.50 个百分点 */
    private BigDecimal vacancyRateYoY;
}
