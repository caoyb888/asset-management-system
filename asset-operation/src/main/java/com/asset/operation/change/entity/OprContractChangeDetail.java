package com.asset.operation.change.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 合同变更明细表（字段级变更前后对比）- 对应 opr_contract_change_detail */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_contract_change_detail")
public class OprContractChangeDetail extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 变更单ID */
    private Long changeId;
    /** 变更字段名（如 contractEnd） */
    private String fieldName;
    /** 字段中文名（如 合同到期日） */
    private String fieldLabel;
    /** 变更前值 */
    private String oldValue;
    /** 变更后值 */
    private String newValue;
    /** 数据类型（string/decimal/date等） */
    private String dataType;
}
