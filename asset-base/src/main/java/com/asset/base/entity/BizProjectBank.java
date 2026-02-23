package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目银行账号实体 - 对应 biz_project_bank 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project_bank")
public class BizProjectBank extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 开户银行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 账户名称 */
    private String accountName;

    /** 是否默认账户：0否 1是 */
    private Integer isDefault;
}
