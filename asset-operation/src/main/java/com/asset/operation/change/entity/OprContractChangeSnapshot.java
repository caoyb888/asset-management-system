package com.asset.operation.change.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 合同变更快照表（变更前后完整快照，支持回溯）- 对应 opr_contract_change_snapshot */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_contract_change_snapshot")
public class OprContractChangeSnapshot extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 变更单ID */
    private Long changeId;
    /** 快照类型（1合同主表/2费项/3应收） */
    private Integer snapshotType;
    /** 快照数据（JSON序列化的完整对象） */
    private String snapshotData;
}
