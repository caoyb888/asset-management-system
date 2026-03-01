package com.asset.system.role.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色表 sys_role */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 */
    private String roleCode;

    /** 数据权限: 1全部 2自定义 3本部门 4本部门及以下 5本人 */
    private Integer dataScope;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 0停用 1正常 */
    private Integer status;

    /** 备注 */
    private String remark;
}
