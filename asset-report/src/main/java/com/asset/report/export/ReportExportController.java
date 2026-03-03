package com.asset.report.export;

import com.asset.common.model.R;
import com.asset.report.entity.RptGenerationLog;
import com.asset.report.export.dto.ExportTaskDTO;
import com.asset.report.export.vo.ExportTaskStatusVO;
import com.asset.report.common.permission.RptDataScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 报表导出接口
 * <p>
 * 路径前缀：/rpt/common/export
 * 采用「提交 → 轮询 → 下载」三步异步模式。
 * </p>
 *
 * <h3>调用流程</h3>
 * <ol>
 *   <li>POST /rpt/common/export → 返回 logCode</li>
 *   <li>GET  /rpt/common/export/{logCode}/status → 轮询直到 status != 2</li>
 *   <li>GET  /rpt/common/export/{logCode}/download → 下载文件（status=1 时有效）</li>
 * </ol>
 */
@Tag(name = "报表导出", description = "异步 Excel/PDF 导出，支持进度查询和文件下载")
@Slf4j
@RestController
@RequestMapping("/rpt/common/export")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService exportService;
    private final com.asset.report.mapper.rpt.RptGenerationLogMapper logMapper;

    /**
     * 提交导出任务
     * <p>
     * 相同 reportCode + format + params 在 30 分钟内命中缓存时直接返回已有 logCode，
     * 否则创建新任务并返回新 logCode，此时文件正在后台生成（status=2）。
     * </p>
     */
    @Operation(summary = "提交导出任务",
            description = "返回 logCode，前端凭此 logCode 轮询 /status 接口")
    @PostMapping
    @RptDataScope
    public R<String> submit(@RequestBody ExportTaskDTO dto) {
        if (dto.getReportCode() == null || dto.getReportCode().isBlank()) {
            return R.fail("reportCode 不能为空");
        }
        String logCode = exportService.submitExport(dto);
        return R.ok(logCode);
    }

    /**
     * 查询导出任务状态
     * <p>
     * 前端建议每 2 秒轮询一次，直到 status 为 0（失败）或 1（成功）。
     * status=1 时响应体中 downloadUrl 字段可直接用于下载。
     * </p>
     */
    @Operation(summary = "查询导出状态",
            description = "status: 0=失败, 1=成功, 2=进行中；成功时 downloadUrl 可用")
    @GetMapping("/{logCode}/status")
    public R<ExportTaskStatusVO> status(
            @Parameter(description = "任务流水号") @PathVariable String logCode) {
        return R.ok(exportService.queryStatus(logCode));
    }

    /**
     * 下载导出文件
     * <p>
     * 仅当 status=1 时有效，直接以 Content-Disposition: attachment 返回文件流。
     * </p>
     */
    @Operation(summary = "下载导出文件",
            description = "status=1 成功后可调用，直接以附件形式返回文件")
    @GetMapping("/{logCode}/download")
    public void download(
            @Parameter(description = "任务流水号") @PathVariable String logCode,
            HttpServletResponse response) throws Exception {

        RptGenerationLog record = logMapper.selectByLogCode(logCode);
        if (record == null || record.getStatus() != RptGenerationLog.STATUS_SUCCESS) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"code\":404,\"msg\":\"文件不存在或尚未就绪\"}");
            return;
        }

        File file = new File(record.getFilePath());
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_GONE);
            response.getWriter().write("{\"code\":410,\"msg\":\"文件已过期，请重新导出\"}");
            return;
        }

        String encodedName = URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            os.flush();
        }
        log.info("[Export] 下载：logCode={}, file={}", logCode, record.getFileName());
    }

    /**
     * 当前用户导出历史（最近 20 条）
     */
    @Operation(summary = "我的导出记录",
            description = "返回当前登录用户最近 20 条导出历史")
    @GetMapping("/my-logs")
    public R<List<RptGenerationLog>> myLogs() {
        return R.ok(exportService.myLogs(20));
    }
}
