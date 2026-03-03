package com.asset.report.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时推送任务响应 VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "定时推送任务")
public class ScheduleTaskVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务编码")
    private String taskCode;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "关联报表编码")
    private String reportCode;

    @Schema(description = "Cron 表达式")
    private String cronExpression;

    @Schema(description = "收件人邮箱列表")
    private List<String> recipients;

    @Schema(description = "抄送人邮箱列表")
    private List<String> ccRecipients;

    @Schema(description = "导出格式：EXCEL / PDF")
    private String exportFormat;

    @Schema(description = "上次执行时间")
    private LocalDateTime lastRunTime;

    @Schema(description = "下次执行时间")
    private LocalDateTime nextRunTime;

    @Schema(description = "累计执行次数")
    private Integer runCount;

    @Schema(description = "连续失败次数")
    private Integer failCount;

    @Schema(description = "状态：0=禁用，1=启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
