package com.asset.system.user.mapper;

import com.asset.system.post.entity.SysPost;
import com.asset.system.user.entity.SysUserPost;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 用户岗位关联 Mapper */
@Mapper
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {

    @Delete("DELETE FROM sys_user_post WHERE user_id=#{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 查询用户拥有的岗位列表 */
    @Select("SELECT p.* FROM sys_post p JOIN sys_user_post up ON p.id=up.post_id WHERE up.user_id=#{userId} AND p.is_deleted=0")
    List<SysPost> selectPostsByUserId(@Param("userId") Long userId);
}
