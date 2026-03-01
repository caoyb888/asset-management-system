package com.asset.system.dept.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 部门/机构表 sys_dept */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父部门ID（0=顶级） */
    private Long parentId;

    /** 祖级ID列表（逗号分隔） */
    private String ancestors;

    /** 部门名称 */
    private String deptName;

    /** 部门编码 */
    private String deptCode;

    /** 排序 */
    private Integer sortOrder;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态: 0停用 1正常 */
    private Integer status;
}
