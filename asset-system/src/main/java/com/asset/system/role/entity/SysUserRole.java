package com.asset.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 用户角色关联表 sys_user_role */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    private Long userId;
    private Long roleId;
}
