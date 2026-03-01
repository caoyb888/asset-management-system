package com.asset.system.menu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 菜单新增/更新请求 */
@Data
public class MenuCreateDTO {
    private Long id;
    private Long parentId;
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
    private String remark;
}
