package com.asset.system.user.controller;

import com.asset.common.model.R;
import com.asset.system.user.dto.ResetPwdDTO;
import com.asset.system.user.dto.UserCreateDTO;
import com.asset.system.user.dto.UserQueryDTO;
import com.asset.system.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 用户管理接口 */
@Tag(name = "01-用户管理")
@RestController
@RequestMapping("/sys/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "用户列表（分页）")
    @GetMapping
    public R<?> page(UserQueryDTO query) { return R.ok(userService.pageQuery(query)); }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) { return R.ok(userService.getDetailById(id)); }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<?> create(@Valid @RequestBody UserCreateDTO dto) { return R.ok(userService.createUser(dto)); }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody UserCreateDTO dto) {
        dto.setId(id); userService.updateUser(dto); return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) { userService.deleteUser(id); return R.ok(); }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-password")
    public R<?> resetPassword(@Valid @RequestBody ResetPwdDTO dto) { userService.resetPassword(dto); return R.ok(); }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.changeStatus(id, body.get("status")); return R.ok();
    }
}
