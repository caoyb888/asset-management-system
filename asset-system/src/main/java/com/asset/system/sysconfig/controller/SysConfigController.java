package com.asset.system.sysconfig.controller;

import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.asset.system.sysconfig.dto.SysConfigCreateDTO;
import com.asset.system.sysconfig.dto.SysConfigQueryDTO;
import com.asset.system.sysconfig.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 系统参数配置接口 */
@Tag(name = "10-系统参数配置")
@RestController
@RequestMapping("/sys/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @Operation(summary = "分页查询参数列表")
    @GetMapping
    public R<?> page(SysConfigQueryDTO query) {
        return R.ok(configService.pageQuery(query));
    }

    @Operation(summary = "按分组查询参数列表")
    @GetMapping("/group/{group}")
    public R<?> listByGroup(@PathVariable String group) {
        return R.ok(configService.listByGroup(group));
    }

    @Operation(summary = "按键查询参数值（走缓存）")
    @GetMapping("/key/{key}")
    public R<?> getByKey(@PathVariable String key) {
        String value = configService.getValueByKey(key);
        return value != null ? R.ok(value) : R.fail(404, "参数不存在");
    }

    @Operation(summary = "新增参数")
    @OperLog(module = "系统配置", action = "新增参数", type = OperLog.OperType.CREATE)
    @PostMapping
    public R<?> create(@Valid @RequestBody SysConfigCreateDTO dto) {
        return R.ok(configService.createConfig(dto));
    }

    @Operation(summary = "更新参数")
    @OperLog(module = "系统配置", action = "更新参数", type = OperLog.OperType.UPDATE)
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody SysConfigCreateDTO dto) {
        dto.setId(id);
        configService.updateConfig(dto);
        return R.ok();
    }

    @Operation(summary = "删除参数（内置参数禁止删除）")
    @OperLog(module = "系统配置", action = "删除参数", type = OperLog.OperType.DELETE)
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        configService.deleteConfig(id);
        return R.ok();
    }

    @Operation(summary = "刷新所有参数缓存")
    @OperLog(module = "系统配置", action = "刷新缓存", type = OperLog.OperType.OTHER)
    @PostMapping("/refresh")
    public R<?> refresh() {
        configService.refreshCache();
        return R.ok();
    }
}
