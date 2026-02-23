package com.asset.investment.opening.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/** 开业审批主表实体 - 对应 inv_opening_approval 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "inv_opening_approval", autoResultMap = true)
public class InvOpeningApproval extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String approvalCode;
    private Long projectId;
    private Long buildingId;
    private Long floorId;
    private Long shopId;
    private Long contractId;
    private Long merchantId;
    private LocalDate plannedOpeningDate;
    private LocalDate actualOpeningDate;
    /** 状态: 0待提交/1审批中/2通过/3驳回 */
    private Integer status;
    private String approvalId;
    private String remark;
    private Long previousApprovalId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode snapshotData;
}
