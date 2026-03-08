package com.asset.operation.change.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.util.List;

/** 合同变更主表 - 对应 opr_contract_change */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "opr_contract_change", autoResultMap = true)
public class OprContractChange extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 变更单号（BG + yyyyMMdd + 4位流水） */
    private String changeCode;
    /** 原合同ID */
    private Long contractId;
    /** 关联台账ID */
    private Long ledgerId;
    /** 项目ID */
    private Long projectId;
    /** 状态（0草稿/1审批中/2通过/3驳回） */
    private Integer status;
    /** 变更生效日期 */
    private LocalDate effectiveDate;
    /** 变更原因 */
    private String reason;
    /** OA审批流程实例ID */
    private String approvalId;
    /** 变更影响预览暂存（受影响应收笔数/金额差异），缓存最近一次预览结果 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode impactSummary;

    /** 变更类型编码列表（非数据库字段，分页列表展示用） */
    @TableField(exist = false)
    private List<String> changeTypeCodes;
}
