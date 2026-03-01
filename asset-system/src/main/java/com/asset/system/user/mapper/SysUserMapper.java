package com.asset.system.user.mapper;

import com.asset.system.user.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 用户 Mapper */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 根据用户名查询（含密码字段） */
    @Select("SELECT * FROM sys_user WHERE username=#{username} AND is_deleted=0 LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    /** 查询部门下的用户数量 */
    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id=#{deptId} AND is_deleted=0")
    long countByDeptId(@Param("deptId") Long deptId);

    /** 查询岗位关联的用户数量 */
    @Select("SELECT COUNT(*) FROM sys_user_post WHERE post_id=#{postId}")
    long countByPostId(@Param("postId") Long postId);
}
