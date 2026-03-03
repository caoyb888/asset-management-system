package com.asset.report.schedule;

import com.asset.report.entity.RptScheduleTask;
import com.asset.report.mapper.rpt.RptScheduleTaskMapper;
import com.asset.report.schedule.dto.ScheduleTaskDTO;
import com.asset.report.schedule.vo.ScheduleTaskVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 报表定时推送任务 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleTaskServiceImpl implements ScheduleTaskService {

    private final RptScheduleTaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<ScheduleTaskVO> page(String keyword, int pageNum, int pageSize) {
        Page<RptScheduleTask> page = new Page<>(pageNum, pageSize);
        IPage<RptScheduleTask> result = taskMapper.selectPage(page, keyword);
        return result.convert(this::toVO);
    }

    @Override
    public ScheduleTaskVO getById(Long id) {
        RptScheduleTask entity = taskMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("定时任务不存在：id=" + id);
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ScheduleTaskDTO dto) {
        // 校验 Cron 表达式
        validateCron(dto.getCronExpression());

        // 任务编码自动生成（前缀 SCH_ + UUID 前8位）
        String taskCode = "SCH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        // 保证唯一（极小概率碰撞时重试）
        while (taskMapper.countByTaskCode(taskCode, null) > 0) {
            taskCode = "SCH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }

        LocalDateTime nextRunTime = calcNextRunTime(dto.getCronExpression());

        RptScheduleTask entity = new RptScheduleTask()
                .setTaskCode(taskCode)
                .setVersion(1)
                .setTaskName(dto.getTaskName())
                .setReportId(0L)
                .setReportCode(dto.getReportCode())
                .setCronExpression(dto.getCronExpression())
                .setRecipients(toJson(dto.getRecipients()))
                .setCcRecipients(toJson(dto.getCcRecipients()))
                .setExportFormat(dto.getExportFormat() == null ? "EXCEL" : dto.getExportFormat().toUpperCase())
                .setFilterParams(toJson(dto.getFilterParams()))
                .setNextRunTime(nextRunTime)
                .setRunCount(0)
                .setFailCount(0)
                .setStatus(Boolean.FALSE.equals(dto.getEnabled()) ? RptScheduleTask.STATUS_DISABLED : RptScheduleTask.STATUS_ENABLED);

        taskMapper.insert(entity);
        log.info("[ScheduleTask] 创建定时任务：id={}, code={}, nextRunTime={}", entity.getId(), taskCode, nextRunTime);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ScheduleTaskDTO dto) {
        RptScheduleTask existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("定时任务不存在：id=" + id);
        }
        validateCron(dto.getCronExpression());

        LocalDateTime nextRunTime = calcNextRunTime(dto.getCronExpression());

        taskMapper.update(null, new LambdaUpdateWrapper<RptScheduleTask>()
                .eq(RptScheduleTask::getId, id)
                .set(RptScheduleTask::getTaskName, dto.getTaskName())
                .set(RptScheduleTask::getReportCode, dto.getReportCode())
                .set(RptScheduleTask::getCronExpression, dto.getCronExpression())
                .set(RptScheduleTask::getRecipients, toJson(dto.getRecipients()))
                .set(RptScheduleTask::getCcRecipients, toJson(dto.getCcRecipients()))
                .set(RptScheduleTask::getExportFormat, dto.getExportFormat() == null ? "EXCEL" : dto.getExportFormat().toUpperCase())
                .set(RptScheduleTask::getFilterParams, toJson(dto.getFilterParams()))
                .set(RptScheduleTask::getNextRunTime, nextRunTime)
                .set(RptScheduleTask::getStatus, Boolean.FALSE.equals(dto.getEnabled()) ? RptScheduleTask.STATUS_DISABLED : RptScheduleTask.STATUS_ENABLED));

        log.info("[ScheduleTask] 更新定时任务：id={}, nextRunTime={}", id, nextRunTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        RptScheduleTask existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("定时任务不存在：id=" + id);
        }
        taskMapper.update(null, new LambdaUpdateWrapper<RptScheduleTask>()
                .eq(RptScheduleTask::getId, id)
                .set(RptScheduleTask::getIsDeleted, true));
        log.info("[ScheduleTask] 删除定时任务：id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int toggle(Long id) {
        RptScheduleTask existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("定时任务不存在：id=" + id);
        }
        int newStatus = existing.getStatus() == RptScheduleTask.STATUS_ENABLED
                ? RptScheduleTask.STATUS_DISABLED
                : RptScheduleTask.STATUS_ENABLED;

        // 启用时重新计算 next_run_time
        LocalDateTime nextRunTime = newStatus == RptScheduleTask.STATUS_ENABLED
                ? calcNextRunTime(existing.getCronExpression())
                : existing.getNextRunTime();

        taskMapper.update(null, new LambdaUpdateWrapper<RptScheduleTask>()
                .eq(RptScheduleTask::getId, id)
                .set(RptScheduleTask::getStatus, newStatus)
                .set(RptScheduleTask::getNextRunTime, nextRunTime)
                .set(RptScheduleTask::getFailCount, 0));  // 重置失败计数

        log.info("[ScheduleTask] 切换状态：id={}, newStatus={}", id, newStatus);
        return newStatus;
    }

    // ──── 工具方法 ────

    /**
     * 计算 Cron 表达式的下次触发时间（Asia/Shanghai 时区）
     * 支持 6 位（Spring 格式：秒 分 时 日 月 周）
     */
    public static LocalDateTime calcNextRunTime(String cronExpr) {
        try {
            CronExpression expr = CronExpression.parse(cronExpr);
            ZonedDateTime next = expr.next(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")));
            return next == null ? null : next.toLocalDateTime();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cron 表达式解析失败：" + cronExpr + "，请使用6位Spring格式（秒 分 时 日 月 周）", e);
        }
    }

    private void validateCron(String cronExpr) {
        try {
            CronExpression.parse(cronExpr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cron 表达式无效：" + cronExpr + "，示例（每天8:30）：0 30 8 * * ?");
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private ScheduleTaskVO toVO(RptScheduleTask e) {
        return new ScheduleTaskVO()
                .setId(e.getId())
                .setTaskCode(e.getTaskCode())
                .setTaskName(e.getTaskName())
                .setReportCode(e.getReportCode())
                .setCronExpression(e.getCronExpression())
                .setRecipients(fromJson(e.getRecipients()))
                .setCcRecipients(fromJson(e.getCcRecipients()))
                .setExportFormat(e.getExportFormat())
                .setLastRunTime(e.getLastRunTime())
                .setNextRunTime(e.getNextRunTime())
                .setRunCount(e.getRunCount())
                .setFailCount(e.getFailCount())
                .setStatus(e.getStatus())
                .setCreatedAt(e.getCreatedAt());
    }
}
