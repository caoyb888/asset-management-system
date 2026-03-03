package com.asset.report.export.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 导出任务状态 VO（前端轮询用）
 */
@Data
@Accessors(chain = true)
@Schema(description = "导出任务状态")
public class ExportTaskStatusVO {

    @Schema(description = "日志流水号（任务ID）")
    private String logCode;

    @Schema(description = "状态：0=失败，1=成功，2=进行中")
    private Integer status;

    @Schema(description = "状态描述：PENDING / SUCCESS / FAIL")
    private String statusName;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "导出数据条数")
    private Integer dataCount;

    @Schema(description = "导出耗时（毫秒）")
    private Integer durationMs;

    @Schema(description = "错误信息（失败时）")
    private String errorMsg;

    @Schema(description = "下载地址（成功后可用）：/rpt/common/export/{logCode}/download")
    private String downloadUrl;

    public static ExportTaskStatusVO pending(String logCode) {
        return new ExportTaskStatusVO()
                .setLogCode(logCode)
                .setStatus(2)
                .setStatusName("PENDING");
    }

    public static ExportTaskStatusVO success(String logCode, String fileName, Integer dataCount,
                                             Integer durationMs) {
        return new ExportTaskStatusVO()
                .setLogCode(logCode)
                .setStatus(1)
                .setStatusName("SUCCESS")
                .setFileName(fileName)
                .setDataCount(dataCount)
                .setDurationMs(durationMs)
                .setDownloadUrl("/rpt/common/export/" + logCode + "/download");
    }

    public static ExportTaskStatusVO fail(String logCode, String errorMsg) {
        return new ExportTaskStatusVO()
                .setLogCode(logCode)
                .setStatus(0)
                .setStatusName("FAIL")
                .setErrorMsg(errorMsg);
    }
}
