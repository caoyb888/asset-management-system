package com.asset.system.dept.mapper;

import com.asset.system.dept.entity.SysDept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 部门 Mapper */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /** 查询子部门数量 */
    @Select("SELECT COUNT(*) FROM sys_dept WHERE parent_id=#{parentId} AND is_deleted=0")
    long countChildren(@Param("parentId") Long parentId);

    /** 查询所有正常状态部门（用于构建树） */
    @Select("SELECT * FROM sys_dept WHERE is_deleted=0 ORDER BY sort_order ASC, id ASC")
    List<SysDept> selectAllNormal();
}
