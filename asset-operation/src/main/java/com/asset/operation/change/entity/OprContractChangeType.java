package com.asset.operation.change.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 合同变更类型关联表 - 对应 opr_contract_change_type（支持多选变更类型） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_contract_change_type")
public class OprContractChangeType extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 变更单ID */
    private Long changeId;
    /** 变更类型编码（RENT/BRAND/TENANT/FEE/CLAUSE/TERM/AREA/COMPANY） */
    private String changeTypeCode;
}
