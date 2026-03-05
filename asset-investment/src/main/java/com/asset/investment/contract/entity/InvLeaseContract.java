package com.asset.investment.contract.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 租赁合同主表实体 - 对应 inv_lease_contract 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_lease_contract")
public class InvLeaseContract extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractCode;
    private String contractName;
    private Long projectId;
    /** 项目名称（联表查询冗余，非 DB 字段） */
    @TableField(exist = false)
    private String projectName;
    private Long merchantId;
    /** 商家名称（联表查询冗余，非 DB 字段） */
    @TableField(exist = false)
    private String merchantName;
    private Long brandId;
    /** 品牌名称（联表查询冗余，非 DB 字段） */
    @TableField(exist = false)
    private String brandName;
    private Long intentionId;
    private String signingEntity;
    private Integer contractType;
    private Long rentSchemeId;
    private LocalDate deliveryDate;
    private LocalDate decorationStart;
    private LocalDate decorationEnd;
    private LocalDate openingDate;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    /** 支付周期: 1月付/2两月付/3季付/4四月付/5半年付/6年付 */
    private Integer paymentCycle;
    /** 账期模式: 1预付/2当期/3后付 */
    private Integer billingMode;
    /** 状态: 0草稿/1审批中/2生效/3到期/4终止/5已删除 */
    private Integer status;
    private BigDecimal totalAmount;
    private String contractText;
    private String approvalId;
    private Integer version;
    private Integer isCurrent;
    private String lockToken;
}
