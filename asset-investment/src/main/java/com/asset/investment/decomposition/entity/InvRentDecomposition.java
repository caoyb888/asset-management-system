package com.asset.investment.decomposition.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.Map;

/** 租金分解主表实体 - 对应 inv_rent_decomposition 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "inv_rent_decomposition", autoResultMap = true)
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
    /** 租决政策关键参数快照（创建时固化，防止政策修改影响历史分解） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> policySnapshot;
}
