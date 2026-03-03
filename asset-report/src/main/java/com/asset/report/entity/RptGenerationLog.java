package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 报表生成日志表（rpt_generation_log）
 * <p>
 * 记录手动/定时导出历史，支持状态轮询和文件下载。
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName("rpt_generation_log")
public class RptGenerationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日志流水号（全局唯一，前端轮询用），如 LOG_20260303143000_001 */
    private String logCode;

    /** 报表ID（关联 rpt_config.id，ad-hoc 导出时为 0） */
    private Long reportId;

    /** 定时任务ID（手动触发时为 null） */
    private Long taskId;

    /** 生成类型：MANUAL / SCHEDULE */
    private String generationType;

    /** 触发人ID（系统自动触发时为 0） */
    private Long triggeredBy;

    /** 文件格式：EXCEL / PDF */
    private String fileFormat;

    /** 文件名称（含扩展名） */
    private String fileName;

    /** 文件本地存储路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件 MD5 校验值 */
    private String fileMd5;

    /** 查询参数快照（JSON），用于相同参数复用 */
    private String filterParams;

    /** 导出数据条数 */
    private Integer dataCount;

    /**
     * 状态：0=失败，1=成功，2=进行中
     */
    private Integer status;

    /** 错误信息（失败时记录） */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Integer durationMs;

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

    // ==================== 状态常量 ====================

    public static final int STATUS_FAIL    = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_PENDING = 2;

    public static final String TYPE_MANUAL   = "MANUAL";
    public static final String TYPE_SCHEDULE = "SCHEDULE";

    public static final String FORMAT_EXCEL = "EXCEL";
    public static final String FORMAT_PDF   = "PDF";
}
