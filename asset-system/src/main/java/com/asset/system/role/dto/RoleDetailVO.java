package com.asset.system.role.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 角色详情 VO */
@Data
public class RoleDetailVO {
    private Long id;
    private String roleName;
    private String roleCode;
    private Integer dataScope;
    private String dataScopeName;
    private Integer sortOrder;
    private Integer status;
    private String statusName;
    private String remark;
    private LocalDateTime createdAt;
    /** 已分配的菜单ID列表 */
    private List<Long> menuIds;
    /** 自定义数据权限部门ID（dataScope=2时有值）*/
    private List<Long> deptIds;
}
