package com.asset.report.export;

import com.asset.report.entity.RptGenerationLog;
import com.asset.report.export.dto.ExportTaskDTO;
import com.asset.report.export.vo.ExportTaskStatusVO;

import java.util.List;

/**
 * 报表导出 Service
 */
public interface ReportExportService {

    /**
     * 提交导出任务（异步执行）
     * <p>
     * 相同参数在 30 分钟内会复用缓存，直接返回已有 logCode。
     * 否则：创建 rpt_generation_log 记录（status=2）→ 提交线程池 → 立即返回 logCode。
     * </p>
     *
     * @param dto 导出请求
     * @return logCode 任务流水号（前端用于轮询状态）
     */
    String submitExport(ExportTaskDTO dto);

    /**
     * 查询导出任务状态
     *
     * @param logCode 任务流水号
     * @return 当前状态 VO
     */
    ExportTaskStatusVO queryStatus(String logCode);

    /**
     * 查询当前登录用户最近 N 条导出记录
     */
    List<RptGenerationLog> myLogs(int limit);

    /**
     * 同步生成导出文件并返回文件路径
     * <p>
     * 供定时推送 Job 调用（非 HTTP 上下文，直接生成文件，不走 Redis 缓存，
     * 调用方负责写 rpt_generation_log）。
     * </p>
     *
     * @param reportCode 报表编码
     * @param paramsJson 筛选参数 JSON 字符串（与 ExportTaskDTO.params 格式一致）
     * @param format     导出格式：EXCEL / PDF
     * @param logCode    关联的日志流水号（文件名前缀）
     * @return 生成文件的本地绝对路径
     */
    String generateFileSync(String reportCode, String paramsJson, String format, String logCode);
}
