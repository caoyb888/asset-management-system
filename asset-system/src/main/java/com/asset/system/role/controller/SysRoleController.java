package com.asset.system.role.controller;

import com.asset.common.model.R;
import com.asset.system.role.dto.RoleCreateDTO;
import com.asset.system.role.dto.RoleQueryDTO;
import com.asset.system.role.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 角色管理接口 */
@Tag(name = "04-角色管理")
@RestController
@RequestMapping("/sys/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色列表（分页）")
    @GetMapping
    public R<?> page(RoleQueryDTO query) { return R.ok(roleService.pageQuery(query)); }

    @Operation(summary = "所有正常状态角色（下拉用）")
    @GetMapping("/list")
    public R<?> list() { return R.ok(roleService.list()); }

    @Operation(summary = "角色详情（含已分配菜单ID）")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) { return R.ok(roleService.getDetailById(id)); }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<?> create(@Valid @RequestBody RoleCreateDTO dto) { return R.ok(roleService.createRole(dto)); }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody RoleCreateDTO dto) {
        dto.setId(id); roleService.updateRole(dto); return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) { roleService.deleteRole(id); return R.ok(); }

    @Operation(summary = "修改角色状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        roleService.changeStatus(id, body.get("status")); return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PutMapping("/{id}/menus")
    public R<?> grantMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.grantMenus(id, body.get("menuIds")); return R.ok();
    }
}
