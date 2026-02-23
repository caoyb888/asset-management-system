package com.asset.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家附件实体 - 对应 biz_merchant_attachment 表
 * <p>
 * 注意：该表无 created_by/updated_by 字段，使用 upload_by 代替上传人；
 * 因此不继承 BaseEntity，手动声明必要的审计字段，并排除 createdBy/updatedBy 的自动填充。
 * </p>
 */
@Data
@TableName("biz_merchant_attachment")
public class BizMerchantAttachment implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家ID */
    private Long merchantId;

    /** 文件名称 */
    private String fileName;

    /** 文件地址 */
    private String fileUrl;

    /** 文件类型（如 pdf/image 等） */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 上传人ID */
    private Long uploadBy;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
