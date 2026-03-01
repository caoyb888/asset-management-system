package com.asset.system.dept.mapper;

import com.asset.system.dept.entity.SysDept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 查询某节点的所有后代（通过 ancestors 路径前缀匹配）
     * 匹配规则：ancestors 字段包含 #{id} 或以 #{id} 开头
     */
    @Select("SELECT * FROM sys_dept WHERE is_deleted=0 AND (ancestors LIKE CONCAT('%,', #{id}, ',%') OR ancestors LIKE CONCAT('%,', #{id}) OR ancestors LIKE CONCAT(#{id}, ',%') OR ancestors = CAST(#{id} AS CHAR))")
    List<SysDept> selectDescendants(@Param("id") Long id);

    /**
     * 批量更新 ancestors 字段：将旧前缀替换为新前缀
     * 用于移动子树时批量修正后代的 ancestors
     */
    @Update("UPDATE sys_dept SET ancestors = REPLACE(ancestors, #{oldPrefix}, #{newPrefix}) WHERE is_deleted=0 AND (ancestors LIKE CONCAT(#{oldPrefix}, ',%') OR ancestors = #{oldPrefix})")
    int batchUpdateAncestors(@Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix);
}
