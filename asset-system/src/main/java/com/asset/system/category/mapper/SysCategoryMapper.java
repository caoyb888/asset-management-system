package com.asset.system.category.mapper;

import com.asset.system.category.entity.SysCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysCategoryMapper extends BaseMapper<SysCategory> {

    /** 查询指定维度的完整树（仅正常状态） */
    @Select("SELECT * FROM sys_category WHERE category_type = #{categoryType} AND is_deleted = 0 AND status = 1 ORDER BY sort_order ASC, id ASC")
    List<SysCategory> selectTreeByType(String categoryType);

    /** 查询子孙节点（通过 ancestors 路径前缀匹配） */
    @Select("SELECT * FROM sys_category WHERE (ancestors LIKE CONCAT(#{ancestors}, ',%') OR ancestors = #{ancestors}) AND category_type = #{categoryType} AND is_deleted = 0")
    List<SysCategory> selectDescendants(String categoryType, String ancestors);

    /** 统计直接子节点数 */
    @Select("SELECT COUNT(*) FROM sys_category WHERE parent_id = #{parentId} AND is_deleted = 0")
    long countChildren(Long parentId);
}
