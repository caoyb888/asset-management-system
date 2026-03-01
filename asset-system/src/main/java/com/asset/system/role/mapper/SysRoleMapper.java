package com.asset.system.role.mapper;

import com.asset.system.role.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 角色 Mapper */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查询用户拥有的角色列表 */
    @Select("SELECT r.* FROM sys_role r JOIN sys_user_role ur ON r.id=ur.role_id WHERE ur.user_id=#{userId} AND r.is_deleted=0")
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
