package com.asset.system.log.controller;

import com.asset.common.model.R;
import com.asset.system.log.dto.LoginLogQueryDTO;
import com.asset.system.log.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 登录日志接口 */
@Tag(name = "07b-登录日志")
@RestController
@RequestMapping("/sys/login-logs")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    @Operation(summary = "登录日志列表（分页）")
    @GetMapping
    public R<?> page(LoginLogQueryDTO query) {
        return R.ok(loginLogService.pageQuery(query));
    }

    @Operation(summary = "清空所有登录日志")
    @DeleteMapping("/clear")
    public R<?> clear() {
        loginLogService.clearAll();
        return R.ok();
    }
}
