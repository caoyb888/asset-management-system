package com.asset.system.log.controller;

import com.asset.common.model.R;
import com.asset.system.log.dto.OperLogQueryDTO;
import com.asset.system.log.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 操作日志接口 */
@Tag(name = "07-操作日志")
@RestController
@RequestMapping("/sys/logs")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService logService;

    @Operation(summary = "操作日志列表（分页）")
    @GetMapping
    public R<?> page(OperLogQueryDTO query) { return R.ok(logService.pageQuery(query)); }

    @Operation(summary = "清空所有操作日志")
    @DeleteMapping("/clear")
    public R<?> clear() { logService.clearAll(); return R.ok(); }
}
