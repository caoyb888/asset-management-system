package com.asset.investment.intention.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 意向协议主表实体 - 对应 inv_intention 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_intention")
public class InvIntention extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 意向协议编号（系统自动生成） */
    private String intentionCode;

    /** 意向协议名称 */
    private String intentionName;

    /** 所属项目ID */
    private Long projectId;

    /** 项目名称（联表查询冗余，非 DB 字段） */
    @TableField(exist = false)
    private String projectName;

    /** 商家ID */
    private Long merchantId;

    /** 商家名称（联表查询冗余，非 DB 字段） */
    @TableField(exist = false)
    private String merchantName;

    /** 意向品牌ID */
    private Long brandId;

    /** 品牌名称（联表查询冗余，非 DB 字段） */
    @TableField(exist = false)
    private String brandName;

    /** 签约主体 */
    private String signingEntity;

    /** 计租方案ID */
    private Long rentSchemeId;

    /** 交付日 */
    private LocalDate deliveryDate;

    /** 装修开始日期 */
    private LocalDate decorationStart;

    /** 装修结束日期 */
    private LocalDate decorationEnd;

    /** 开业日 */
    private LocalDate openingDate;

    /** 合同开始日期 */
    private LocalDate contractStart;

    /** 合同结束日期 */
    private LocalDate contractEnd;

    /**
     * 支付周期
     * 1月付/2两月付/3季付/4四月付/5半年付/6年付
     */
    private Integer paymentCycle;

    /**
     * 账期模式
     * 1预付/2当期/3后付
     */
    private Integer billingMode;

    /**
     * 状态
     * 0草稿/1审批中/2审批通过/3驳回/4已转合同/5已删除
     */
    private Integer status;

    /** 费用总额 */
    private BigDecimal totalAmount;

    /** 协议文本内容 */
    private String agreementText;

    /** 审批流程实例ID */
    private String approvalId;

    /** 版本号 */
    private Integer version;

    /** 是否当前版本: 1是/0否 */
    private Integer isCurrent;
}
