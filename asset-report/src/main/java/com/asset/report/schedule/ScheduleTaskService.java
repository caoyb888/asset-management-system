package com.asset.report.schedule;

import com.asset.report.schedule.dto.ScheduleTaskDTO;
import com.asset.report.schedule.vo.ScheduleTaskVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 报表定时推送任务 Service
 */
public interface ScheduleTaskService {

    /**
     * 分页查询任务列表
     *
     * @param keyword 搜索关键字（任务名称 / 报表编码）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     */
    IPage<ScheduleTaskVO> page(String keyword, int pageNum, int pageSize);

    /**
     * 根据ID查询单条任务
     */
    ScheduleTaskVO getById(Long id);

    /**
     * 创建定时推送任务
     *
     * @return 新记录 ID
     */
    Long create(ScheduleTaskDTO dto);

    /**
     * 更新定时推送任务
     */
    void update(Long id, ScheduleTaskDTO dto);

    /**
     * 删除任务（逻辑删除）
     */
    void delete(Long id);

    /**
     * 切换启用/禁用状态
     *
     * @return 操作后的状态（0=禁用，1=启用）
     */
    int toggle(Long id);
}
