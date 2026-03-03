package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 报表定时推送任务表（rpt_schedule_task）
 * <p>
 * 由 {@link com.asset.report.schedule.ScheduledReportJobHandler}
 * 每分钟扫描 next_run_time <= now 的启用任务并执行。
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName("rpt_schedule_task")
public class RptScheduleTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编码（全局唯一） */
    private String taskCode;

    /** 版本号（支持同编码多版本） */
    private Integer version;

    /** 任务名称（人读） */
    private String taskName;

    /** 关联报表配置ID（rpt_config.id，无关联时为 0） */
    private Long reportId;

    /** 关联报表编码（冗余，与 ExportTaskDTO.reportCode 对应） */
    private String reportCode;

    /** Cron 表达式（6位Spring格式，秒 分 时 日 月 周） */
    private String cronExpression;

    /** 收件人邮箱列表（JSON数组字符串，如 ["a@b.com","c@d.com"]） */
    private String recipients;

    /** 抄送人邮箱列表（JSON数组字符串，可为 null） */
    private String ccRecipients;

    /** 导出格式：EXCEL / PDF */
    private String exportFormat;

    /** 固定筛选参数（JSON对象字符串，与 ExportTaskDTO.params 格式一致） */
    private String filterParams;

    /** 上次执行时间 */
    private LocalDateTime lastRunTime;

    /** 下次执行时间（由 Cron 解析计算，每次执行后更新） */
    private LocalDateTime nextRunTime;

    /** 累计执行次数 */
    private Integer runCount;

    /** 连续失败次数（执行成功后归零；≥3 次自动禁用） */
    private Integer failCount;

    /** 状态：0=禁用，1=启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;

    // ==================== 常量 ====================

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED  = 1;

    public static final String FORMAT_EXCEL = "EXCEL";
    public static final String FORMAT_PDF   = "PDF";
}
