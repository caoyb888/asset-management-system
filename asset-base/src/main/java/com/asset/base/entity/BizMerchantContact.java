package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家联系人实体 - 对应 biz_merchant_contact 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_merchant_contact")
public class BizMerchantContact extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家ID */
    private Long merchantId;

    /** 联系人姓名 */
    private String contactName;

    /** 电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 职位 */
    private String position;

    /** 是否主要联系人：0否 1是 */
    private Integer isPrimary;
}
