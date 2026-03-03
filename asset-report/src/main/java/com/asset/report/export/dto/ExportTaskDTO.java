package com.asset.report.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 报表导出任务提交 DTO
 */
@Data
@Schema(description = "报表导出请求")
public class ExportTaskDTO {

    @Schema(description = "报表编码，见 ReportExportCodes 常量", example = "FIN_RECEIVABLE_SUMMARY")
    private String reportCode;

    @Schema(description = "文件格式：EXCEL / PDF", example = "EXCEL")
    private String format;

    /**
     * 查询参数 Map，对应各报表的筛选条件，如：
     * <pre>
     * {
     *   "projectId": 1,
     *   "startMonth": "2025-01",
     *   "endMonth":   "2025-12",
     *   "compareMode": "NONE"
     * }
     * </pre>
     */
    @Schema(description = "查询参数（与报表筛选栏参数一致）")
    private Map<String, Object> params;
}
