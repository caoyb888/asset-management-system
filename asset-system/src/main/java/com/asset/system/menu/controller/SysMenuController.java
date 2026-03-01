package com.asset.system.menu.controller;

import com.asset.common.model.R;
import com.asset.system.menu.dto.MenuCreateDTO;
import com.asset.system.menu.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 菜单管理接口 */
@Tag(name = "05-菜单管理")
@RestController
@RequestMapping("/sys/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "菜单树（全量）")
    @GetMapping
    public R<?> tree() { return R.ok(menuService.getMenuTree()); }

    @Operation(summary = "用户路由树（按userId）")
    @GetMapping("/routes")
    public R<?> routes(@RequestParam Long userId) { return R.ok(menuService.getRouteTree(userId)); }

    @Operation(summary = "用户权限标识（按userId）")
    @GetMapping("/perms")
    public R<?> perms(@RequestParam Long userId) { return R.ok(menuService.getPermsByUserId(userId)); }

    @Operation(summary = "新增菜单")
    @PostMapping
    public R<?> create(@Valid @RequestBody MenuCreateDTO dto) { return R.ok(menuService.createMenu(dto)); }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody MenuCreateDTO dto) {
        dto.setId(id); menuService.updateMenu(dto); return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) { menuService.deleteMenu(id); return R.ok(); }
}
