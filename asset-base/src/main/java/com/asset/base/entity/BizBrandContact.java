package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌联系人实体 - 对应 biz_brand_contact 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_brand_contact")
public class BizBrandContact extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品牌ID */
    private Long brandId;

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
