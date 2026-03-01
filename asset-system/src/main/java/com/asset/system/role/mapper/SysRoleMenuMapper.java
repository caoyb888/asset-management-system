package com.asset.system.role.mapper;

import com.asset.system.role.entity.SysRoleMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 角色菜单关联 Mapper */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    @Delete("DELETE FROM sys_role_menu WHERE role_id=#{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id=#{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
