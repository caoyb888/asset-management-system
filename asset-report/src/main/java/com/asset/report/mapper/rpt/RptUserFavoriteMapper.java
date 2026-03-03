package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptUserFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户报表收藏 Mapper
 */
@Mapper
public interface RptUserFavoriteMapper extends BaseMapper<RptUserFavorite> {

    /**
     * 查询指定用户的所有收藏，按 sort_order 升序
     */
    List<RptUserFavorite> selectByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否已收藏某报表
     */
    int countByUserAndCode(@Param("userId") Long userId, @Param("reportCode") String reportCode);

    /**
     * 批量更新排序（逐条更新）
     */
    int updateSortOrder(@Param("id") Long id, @Param("userId") Long userId, @Param("sortOrder") int sortOrder);
}
