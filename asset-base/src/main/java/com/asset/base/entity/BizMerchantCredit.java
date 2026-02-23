package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 商家诚信记录实体 - 对应 biz_merchant_credit 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_merchant_credit")
public class BizMerchantCredit extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家ID */
    private Long merchantId;

    /**
     * 记录类型
     * 1-好评 2-差评 3-违约 4-其他
     */
    private Integer recordType;

    /** 记录内容 */
    private String content;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 操作人ID */
    private Long operatorId;

    /** 附件地址 */
    private String attachmentUrl;
}
