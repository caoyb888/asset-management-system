package com.asset.operation.termination.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 合同解约主表 - 对应 opr_contract_termination */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_contract_termination")
public class OprContractTermination extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 解约单号（JY + yyyyMMdd + 4位流水） */
    private String terminationCode;
    /** 原合同ID */
    private Long contractId;
    /** 关联台账ID */
    private Long ledgerId;
    /** 项目ID */
    private Long projectId;
    /** 商家ID */
    private Long merchantId;
    /** 品牌ID */
    private Long brandId;
    /** 商铺ID */
    private Long shopId;
    /** 解约类型（1到期/2提前/3重签） */
    private Integer terminationType;
    /** 解约日期 */
    private LocalDate terminationDate;
    /** 解约原因 */
    private String reason;
    /** 重签新合同ID（重签解约时关联） */
    private Long newContractId;
    /** 违约金（提前解约时） */
    private BigDecimal penaltyAmount;
    /** 退还保证金 */
    private BigDecimal refundDeposit;
    /** 未结算应收 */
    private BigDecimal unsettledAmount;
    /** 清算总额（正数应收/负数应退） */
    private BigDecimal settlementAmount;
    /** 状态（0草稿/1审批中/2已生效/3驳回） */
    private Integer status;
    /** OA审批流程ID */
    private String approvalId;
}
