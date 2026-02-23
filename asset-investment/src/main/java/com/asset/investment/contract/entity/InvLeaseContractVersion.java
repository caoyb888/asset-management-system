package com.asset.investment.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.time.LocalDateTime;

/** 合同版本快照表实体 - 对应 inv_lease_contract_version 表 */
@Data
@TableName(value = "inv_lease_contract_version", autoResultMap = true)
public class InvLeaseContractVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Integer version;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode snapshotData;
    private String changeReason;
    private Long createdBy;
    private LocalDateTime createdAt;
}
