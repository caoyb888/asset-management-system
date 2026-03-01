package com.asset.system.role.mapper;

import com.asset.system.role.entity.SysRoleData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 角色数据权限 Mapper */
@Mapper
public interface SysRoleDataMapper extends BaseMapper<SysRoleData> {

    @Delete("DELETE FROM sys_role_data WHERE role_id=#{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT dept_id FROM sys_role_data WHERE role_id=#{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);
}
