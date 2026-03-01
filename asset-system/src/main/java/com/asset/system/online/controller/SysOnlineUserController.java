package com.asset.system.online.controller;

import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.asset.system.auth.service.SysTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 在线用户管理接口
 * <p>
 * 查看当前在线用户列表，支持管理员强制踢出指定用户
 * </p>
 */
@Tag(name = "11-在线用户管理")
@RestController
@RequestMapping("/sys/online-users")
@RequiredArgsConstructor
public class SysOnlineUserController {

    private final SysTokenService tokenService;

    @Operation(summary = "查询在线用户列表")
    @GetMapping
    public R<?> list() {
        return R.ok(tokenService.listOnlineUsers());
    }

    @Operation(summary = "强制下线指定用户")
    @OperLog(module = "在线用户", action = "强制下线", type = OperLog.OperType.DELETE)
    @DeleteMapping("/{userId}")
    public R<?> kick(@PathVariable Long userId) {
        tokenService.removeOnlineSession(userId);
        tokenService.removeAllRefreshTokensByUser(userId);
        return R.ok();
    }
}
