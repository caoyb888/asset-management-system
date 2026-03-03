package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptGenerationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 报表生成日志 Mapper
 */
@Mapper
public interface RptGenerationLogMapper extends BaseMapper<RptGenerationLog> {

    /**
     * 按日志流水号查询（不走逻辑删除过滤，含已删除记录可见下载历史）
     */
    @Select("SELECT * FROM rpt_generation_log WHERE log_code = #{logCode} AND is_deleted = 0 LIMIT 1")
    RptGenerationLog selectByLogCode(@Param("logCode") String logCode);

    /**
     * 查询当前用户最近 N 条导出记录
     */
    @Select("SELECT * FROM rpt_generation_log WHERE triggered_by = #{userId} AND is_deleted = 0 " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    java.util.List<RptGenerationLog> selectByUser(@Param("userId") Long userId,
                                                   @Param("limit") int limit);
}
