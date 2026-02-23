package com.asset.investment.opening.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 开业审批附件表实体 - 对应 inv_opening_attachment 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_opening_attachment")
public class InvOpeningAttachment extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long openingApprovalId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
}
