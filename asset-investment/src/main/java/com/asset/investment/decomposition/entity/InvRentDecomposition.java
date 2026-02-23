package com.asset.investment.decomposition.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 租金分解主表实体 - 对应 inv_rent_decomposition 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_rent_decomposition")
public class InvRentDecomposition extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String decompCode;
    private Long projectId;
    private Long policyId;
    private BigDecimal totalAnnualRent;
    private BigDecimal totalAnnualFee;
    /** 状态: 0草稿/1审批中/2通过/3驳回 */
    private Integer status;
    private String approvalId;
}
