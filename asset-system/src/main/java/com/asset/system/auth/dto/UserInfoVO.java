package com.asset.system.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前用户信息 VO
 */
@Data
@Builder
public class UserInfoVO {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名/昵称 */
    private String realName;

    /** 头像 URL */
    private String avatar;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 角色编码列表 */
    private List<String> roles;

    /** 权限标识列表（如 sys:user:list） */
    private List<String> permissions;
}
