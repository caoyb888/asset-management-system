package com.asset.report.schedule;

import com.asset.report.entity.RptGenerationLog;
import com.asset.report.entity.RptScheduleTask;
import com.asset.report.export.ReportExportService;
import com.asset.report.mapper.rpt.RptGenerationLogMapper;
import com.asset.report.mapper.rpt.RptScheduleTaskMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 报表定时推送 XXL-Job Handler
 * <p>
 * JobHandler 名称：{@code scheduledReportPushJob}
 * 推荐在 XXL-Job 控制台配置为：每分钟执行一次（Cron: 0 * * * * ?）
 * </p>
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li>查询所有 status=1 且 next_run_time &le; now 的任务</li>
 *   <li>逐任务生成 Excel 文件（同步）</li>
 *   <li>通过 Spring Mail 发送带附件邮件给收件人</li>
 *   <li>更新 last_run_time / next_run_time / run_count，成功时 fail_count 归零</li>
 *   <li>失败时 fail_count++ ；连续失败 &ge; disableThreshold 次后自动禁用任务</li>
 *   <li>无论成功或失败均写入 rpt_generation_log</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledReportJobHandler {

    private final RptScheduleTaskMapper taskMapper;
    private final RptGenerationLogMapper logMapper;
    private final ReportExportService exportService;
    private final MailService mailService;
    private final ObjectMapper objectMapper;

    @Value("${report.schedule.max-retry:3}")
    private int maxRetry;

    @Value("${report.schedule.disable-threshold:3}")
    private int disableThreshold;

    private static final DateTimeFormatter LOG_CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    /**
     * 每分钟扫描并执行到期的定时推送任务
     */
    @XxlJob("scheduledReportPushJob")
    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        List<RptScheduleTask> dueTasks = taskMapper.selectDueTasks(now);

        if (dueTasks.isEmpty()) {
            log.debug("[ScheduledJob] 无到期任务，跳过");
            return;
        }

        log.info("[ScheduledJob] 扫描到 {} 个到期任务，开始执行", dueTasks.size());

        for (RptScheduleTask task : dueTasks) {
            executeTask(task);
        }
    }

    // ==================== 单任务执行 ====================

    private void executeTask(RptScheduleTask task) {
        log.info("[ScheduledJob] 开始执行：taskId={}, reportCode={}", task.getId(), task.getReportCode());
        long startMs = System.currentTimeMillis();
        String logCode = generateLogCode();

        // 预写日志（进行中状态）
        RptGenerationLog genLog = new RptGenerationLog()
                .setLogCode(logCode)
                .setReportId(task.getReportId() == null ? 0L : task.getReportId())
                .setTaskId(task.getId())
                .setGenerationType(RptGenerationLog.TYPE_SCHEDULE)
                .setTriggeredBy(0L)
                .setFileFormat(task.getExportFormat())
                .setFilterParams(task.getFilterParams())
                .setDataCount(0)
                .setStatus(RptGenerationLog.STATUS_PENDING);
        logMapper.insert(genLog);

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                // 1. 生成文件
                String filePath = exportService.generateFileSync(
                        task.getReportCode(),
                        task.getFilterParams(),
                        task.getExportFormat(),
                        logCode);

                File file = new File(filePath);

                // 2. 发送邮件
                List<String> recipients = parseJsonList(task.getRecipients());
                List<String> ccList = parseJsonList(task.getCcRecipients());
                String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                String subject = "【报表推送】" + task.getTaskName() + " - " + now;
                String body = MailService.buildReportMailBody(
                        task.getTaskName(), task.getReportCode(), file.getName(), now);
                mailService.sendWithAttachment(recipients, ccList, subject, body, file);

                // 3. 更新任务状态（成功）
                long elapsed = System.currentTimeMillis() - startMs;
                LocalDateTime nextRunTime = ScheduleTaskServiceImpl.calcNextRunTime(task.getCronExpression());
                taskMapper.update(null, new LambdaUpdateWrapper<RptScheduleTask>()
                        .eq(RptScheduleTask::getId, task.getId())
                        .set(RptScheduleTask::getLastRunTime, LocalDateTime.now())
                        .set(RptScheduleTask::getNextRunTime, nextRunTime)
                        .set(RptScheduleTask::getRunCount, task.getRunCount() + 1)
                        .set(RptScheduleTask::getFailCount, 0));  // 成功归零

                // 4. 更新日志为成功
                logMapper.update(null, new LambdaUpdateWrapper<RptGenerationLog>()
                        .eq(RptGenerationLog::getLogCode, logCode)
                        .set(RptGenerationLog::getStatus, RptGenerationLog.STATUS_SUCCESS)
                        .set(RptGenerationLog::getFilePath, filePath)
                        .set(RptGenerationLog::getFileName, file.getName())
                        .set(RptGenerationLog::getFileSize, file.length())
                        .set(RptGenerationLog::getDurationMs, (int) elapsed)
                        .set(RptGenerationLog::getUpdatedAt, LocalDateTime.now()));

                log.info("[ScheduledJob] 执行成功：taskId={}, elapsed={}ms, nextRunTime={}",
                        task.getId(), elapsed, nextRunTime);
                return;

            } catch (Exception e) {
                lastException = e;
                log.warn("[ScheduledJob] 第 {}/{} 次尝试失败：taskId={}, error={}",
                        attempt, maxRetry, task.getId(), e.getMessage());
                if (attempt < maxRetry) {
                    try { Thread.sleep(3000L * attempt); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 所有重试均失败
        long elapsed = System.currentTimeMillis() - startMs;
        int newFailCount = task.getFailCount() + 1;
        int newStatus = newFailCount >= disableThreshold
                ? RptScheduleTask.STATUS_DISABLED : RptScheduleTask.STATUS_ENABLED;

        taskMapper.update(null, new LambdaUpdateWrapper<RptScheduleTask>()
                .eq(RptScheduleTask::getId, task.getId())
                .set(RptScheduleTask::getLastRunTime, LocalDateTime.now())
                .set(RptScheduleTask::getFailCount, newFailCount)
                .set(RptScheduleTask::getStatus, newStatus)
                .set(RptScheduleTask::getRunCount, task.getRunCount() + 1));

        if (newStatus == RptScheduleTask.STATUS_DISABLED) {
            log.error("[ScheduledJob] 任务连续失败 {} 次，已自动禁用：taskId={}",
                    newFailCount, task.getId());
        }

        String errMsg = lastException != null ? lastException.getMessage() : "未知错误";
        if (errMsg != null && errMsg.length() > 500) errMsg = errMsg.substring(0, 500);

        logMapper.update(null, new LambdaUpdateWrapper<RptGenerationLog>()
                .eq(RptGenerationLog::getLogCode, logCode)
                .set(RptGenerationLog::getStatus, RptGenerationLog.STATUS_FAIL)
                .set(RptGenerationLog::getErrorMsg, errMsg)
                .set(RptGenerationLog::getDurationMs, (int) elapsed)
                .set(RptGenerationLog::getUpdatedAt, LocalDateTime.now()));

        log.error("[ScheduledJob] 执行失败（重试 {} 次）：taskId={}, error={}",
                maxRetry, task.getId(), errMsg);
    }

    // ==================== 工具方法 ====================

    private String generateLogCode() {
        String ts = LocalDateTime.now().format(LOG_CODE_FMT);
        int seq = SEQ.incrementAndGet() % 1000;
        return String.format("SCH_%s_%03d", ts, seq);
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
