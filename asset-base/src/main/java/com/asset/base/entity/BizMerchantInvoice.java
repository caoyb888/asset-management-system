package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家开票信息实体 - 对应 biz_merchant_invoice 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_merchant_invoice")
public class BizMerchantInvoice extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家ID */
    private Long merchantId;

    /** 发票抬头 */
    private String invoiceTitle;

    /** 税号 */
    private String taxNumber;

    /** 开户银行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 注册地址 */
    private String address;

    /** 注册电话 */
    private String phone;

    /** 是否默认：0否 1是 */
    private Integer isDefault;
}
