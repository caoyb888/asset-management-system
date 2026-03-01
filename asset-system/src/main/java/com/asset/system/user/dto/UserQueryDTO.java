package com.asset.system.user.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {
    /** 用户名（模糊） */
    private String username;
    /** 真实姓名（模糊） */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 状态 */
    private Integer status;
    /** 部门ID */
    private Long deptId;
    /** 角色ID */
    private Long roleId;
}
