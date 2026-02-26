package com.asset.operation.ledger.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 合同台账主表 - 对应 opr_contract_ledger */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_contract_ledger")
public class OprContractLedger extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 台账编号（系统自动生成） */
    private String ledgerCode;
    /** 关联招商合同ID */
    private Long contractId;
    /** 项目ID */
    private Long projectId;
    /** 商家ID */
    private Long merchantId;
    /** 品牌ID */
    private Long brandId;
    /** 合同类型（1租赁/2联营/3临时） */
    private Integer contractType;
    /** 合同开始日期 */
    private LocalDate contractStart;
    /** 合同到期日期 */
    private LocalDate contractEnd;
    /** 双签状态（0待双签/1已双签） */
    private Integer doubleSignStatus;
    /** 双签完成时间 */
    private LocalDateTime doubleSignDate;
    /** 应收生成状态（0未生成/1已生成/2已推送） */
    private Integer receivableStatus;
    /** 审核状态（0待审核/1通过/2驳回） */
    private Integer auditStatus;
    /** 台账状态（0进行中/1已完成/2已解约） */
    private Integer status;
    /** 应收推送时间 */
    private LocalDateTime pushTime;
}
