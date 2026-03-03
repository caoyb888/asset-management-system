package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptScheduleTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表定时推送任务 Mapper
 */
@Mapper
public interface RptScheduleTaskMapper extends BaseMapper<RptScheduleTask> {

    /**
     * 分页查询（支持关键字搜索任务名称）
     */
    IPage<RptScheduleTask> selectPage(Page<RptScheduleTask> page,
                                      @Param("keyword") String keyword);

    /**
     * 查询所有到期的启用任务（next_run_time <= now）
     */
    List<RptScheduleTask> selectDueTasks(@Param("now") LocalDateTime now);

    /**
     * 检查任务编码是否已存在（同编码 + 未删除）
     */
    int countByTaskCode(@Param("taskCode") String taskCode,
                        @Param("excludeId") Long excludeId);
}
