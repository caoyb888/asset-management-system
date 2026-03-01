package com.asset.system.menu.mapper;

import com.asset.system.menu.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 菜单 Mapper */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 查询角色拥有的权限标识列表 */
    @Select("SELECT DISTINCT m.perms FROM sys_menu m JOIN sys_role_menu rm ON m.id=rm.menu_id JOIN sys_user_role ur ON rm.role_id=ur.role_id WHERE ur.user_id=#{userId} AND m.perms!='' AND m.status=1")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询用户可见的菜单列表（用于生成路由） */
    @Select("SELECT DISTINCT m.* FROM sys_menu m JOIN sys_role_menu rm ON m.id=rm.menu_id JOIN sys_user_role ur ON rm.role_id=ur.role_id WHERE ur.user_id=#{userId} AND m.menu_type IN ('M','C') AND m.status=1 AND m.visible=1 ORDER BY m.parent_id, m.sort_order")
    List<SysMenu> selectRoutesByUserId(@Param("userId") Long userId);

    /** 查询子菜单数量 */
    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id=#{parentId}")
    long countChildren(@Param("parentId") Long parentId);
}
