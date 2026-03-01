package com.asset.system.user.controller;

import com.asset.common.model.R;
import com.asset.common.security.util.SecurityUtil;
import com.asset.system.user.dto.ChangePasswordDTO;
import com.asset.system.user.dto.ResetPwdDTO;
import com.asset.system.user.dto.UserCreateDTO;
import com.asset.system.user.dto.UserProfileDTO;
import com.asset.system.user.dto.UserQueryDTO;
import com.asset.system.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @Operation(summary = "重置密码（管理员操作）")
    @PutMapping("/reset-password")
    public R<?> resetPassword(@Valid @RequestBody ResetPwdDTO dto) { userService.resetPassword(dto); return R.ok(); }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.changeStatus(id, body.get("status")); return R.ok();
    }

    // ─── 个人资料（当前登录用户） ────────────────────────────────────────────────

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public R<?> updateProfile(@RequestBody UserProfileDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.updateProfile(userId, dto);
        return R.ok();
    }

    @Operation(summary = "修改自身密码")
    @PutMapping("/profile/password")
    public R<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.changePassword(userId, dto);
        return R.ok();
    }

    // ─── 角色 / 岗位分配 ──────────────────────────────────────────────────────

    @Operation(summary = "分配角色")
    @PostMapping("/{id}/roles")
    public R<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        userService.assignRoles(id, body.get("roleIds")); return R.ok();
    }

    @Operation(summary = "分配岗位")
    @PostMapping("/{id}/posts")
    public R<?> assignPosts(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        userService.assignPosts(id, body.get("postIds")); return R.ok();
    }

    // ─── 强制下线 ─────────────────────────────────────────────────────────────

    @Operation(summary = "强制用户下线")
    @DeleteMapping("/{id}/token")
    public R<?> forceOffline(@PathVariable Long id) {
        userService.forceOffline(id); return R.ok();
    }
}
