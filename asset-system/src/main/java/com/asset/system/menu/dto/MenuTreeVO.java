package com.asset.system.menu.dto;

import lombok.Data;

import java.util.List;

/** 菜单树节点 VO */
@Data
public class MenuTreeVO {
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
    private List<MenuTreeVO> children;
}
