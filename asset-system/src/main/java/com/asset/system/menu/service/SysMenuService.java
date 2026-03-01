package com.asset.system.menu.service;

import com.asset.system.menu.dto.MenuCreateDTO;
import com.asset.system.menu.dto.MenuTreeVO;
import com.asset.system.menu.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 菜单管理 Service */
public interface SysMenuService extends IService<SysMenu> {
    /** 获取完整菜单树 */
    List<MenuTreeVO> getMenuTree();
    /** 获取用户权限路由树 */
    List<MenuTreeVO> getRouteTree(Long userId);
    /** 获取用户权限标识列表 */
    List<String> getPermsByUserId(Long userId);
    Long createMenu(MenuCreateDTO dto);
    void updateMenu(MenuCreateDTO dto);
    void deleteMenu(Long id);
}
