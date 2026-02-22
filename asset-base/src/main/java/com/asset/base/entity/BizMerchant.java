package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家实体 - 对应 biz_merchant 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_merchant")
public class BizMerchant extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 商家编号 */
    private String merchantCode;

    /** 商家名称 */
    private String merchantName;

    /**
     * 商家属性
     * 1-个体户 2-企业
     */
    private Integer merchantAttr;

    /**
     * 商家性质
     * 1-民营 2-国营 3-外资 4-合资
     */
    private Integer merchantNature;

    /** 经营业态 */
    private String formatType;

    /** 自然人姓名 */
    private String naturalPerson;

    /** 身份证号（SM4加密存储） */
    private String idCard;

    /** 地址 */
    private String address;

    /** 手机 */
    private String phone;

    /**
     * 商家评级
     * 1-优秀 2-良好 3-一般 4-差
     */
    private Integer merchantLevel;

    /**
     * 审核状态
     * 0-待审核 1-通过 2-驳回
     */
    private Integer auditStatus;
}
