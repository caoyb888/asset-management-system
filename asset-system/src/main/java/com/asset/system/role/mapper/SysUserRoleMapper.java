package com.asset.system.role.mapper;

import com.asset.system.role.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用户角色关联 Mapper */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id=#{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE role_id=#{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 统计拥有该角色的用户数量 */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id=#{roleId}")
    long countByRoleId(@Param("roleId") Long roleId);
}
