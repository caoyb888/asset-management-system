package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目财务联系人实体 - 对应 biz_project_finance_contact 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project_finance_contact")
public class BizProjectFinanceContact extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 联系人姓名 */
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 信用代码 */
    private String creditCode;

    /** 印章类型 */
    private String sealType;

    /** 印章说明 */
    private String sealDesc;
}
