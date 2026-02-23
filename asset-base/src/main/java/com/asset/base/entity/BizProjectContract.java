package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目合同甲方信息实体 - 对应 biz_project_contract 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project_contract")
public class BizProjectContract extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 甲方名称 */
    private String partyAName;

    /** 甲方简称 */
    private String partyAAbbr;

    /** 甲方地址 */
    private String partyAAddress;

    /** 甲方电话 */
    private String partyAPhone;

    /** 营业执照号 */
    private String businessLicense;

    /** 法定代表人 */
    private String legalRepresentative;

    /** 邮箱 */
    private String email;
}
