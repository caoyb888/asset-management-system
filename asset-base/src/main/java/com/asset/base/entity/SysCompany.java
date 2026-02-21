package com.asset.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 公司表
 */
@Data
@TableName("sys_company")
public class SysCompany {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司编码 */
    private String companyCode;

    /** 公司名称 */
    private String companyName;

    /** 状态：0停用 1启用 */
    private Integer status;
}
