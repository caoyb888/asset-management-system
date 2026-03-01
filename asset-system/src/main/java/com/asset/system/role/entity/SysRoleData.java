package com.asset.system.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 角色自定义数据权限（部门）关联表 sys_role_data */
@Data
@TableName("sys_role_data")
public class SysRoleData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID */
    private Long roleId;

    /** 部门ID */
    private Long deptId;
}
