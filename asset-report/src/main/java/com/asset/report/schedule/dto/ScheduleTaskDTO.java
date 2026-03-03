package com.asset.report.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 定时推送任务 创建/更新 DTO
 */
@Data
@Schema(description = "定时推送任务创建/更新请求")
public class ScheduleTaskDTO {

    @Schema(description = "任务名称（人读）", example = "每月末财务应收汇总")
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @Schema(description = "关联报表编码（与导出接口 reportCode 一致）", example = "FIN_RECEIVABLE_SUMMARY")
    @NotBlank(message = "reportCode 不能为空")
    private String reportCode;

    @Schema(
        description = "Cron 表达式（6位Spring格式：秒 分 时 日 月 周）",
        example = "0 0 8 28 * ?"
    )
    @NotBlank(message = "cronExpression 不能为空")
    private String cronExpression;

    @Schema(description = "收件人邮箱列表（至少1个）", example = "[\"admin@company.com\"]")
    @NotEmpty(message = "至少指定一个收件人邮箱")
    private List<@Email(message = "收件人邮箱格式不正确") String> recipients;

    @Schema(description = "抄送人邮箱列表（可为空）")
    private List<String> ccRecipients;

    @Schema(description = "导出格式：EXCEL / PDF", example = "EXCEL")
    private String exportFormat = "EXCEL";

    @Schema(
        description = "固定筛选参数（与报表筛选栏参数一致，格式同 ExportTaskDTO.params）",
        example = "{\"startMonth\":\"2026-01\",\"endMonth\":\"2026-12\"}"
    )
    private Map<String, Object> filterParams;

    @Schema(description = "是否立即启用", example = "true")
    private Boolean enabled = true;
}
