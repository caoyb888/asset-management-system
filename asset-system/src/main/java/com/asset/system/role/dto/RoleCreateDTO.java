package com.asset.system.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 角色新增/更新请求 */
@Data
public class RoleCreateDTO {
    private Long id;
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;
    private Integer dataScope;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    /** 分配的菜单ID列表 */
    private List<Long> menuIds;
}
