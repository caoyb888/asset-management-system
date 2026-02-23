package com.asset.investment.config.controller;

import com.asset.common.model.R;
import com.asset.investment.config.entity.CfgRentScheme;
import com.asset.investment.config.service.CfgRentSchemeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 计租方案配置 Controller
 */
@Tag(name = "计租方案配置", description = "招商配置-计租方案增删改查")
@RestController
@RequestMapping("/inv/config/rent-schemes")
@RequiredArgsConstructor
public class CfgRentSchemeController {

    private final CfgRentSchemeService rentSchemeService;

    @Operation(summary = "查询计租方案列表", description = "showAll=true 返回全部（含停用）；默认只返回启用方案")
    @GetMapping
    public R<List<CfgRentScheme>> list(
            @Parameter(description = "是否返回全部方案（含停用），管理页传 true") @RequestParam(defaultValue = "false") boolean showAll) {
        LambdaQueryWrapper<CfgRentScheme> wrapper = new LambdaQueryWrapper<CfgRentScheme>()
                .orderByAsc(CfgRentScheme::getId);
        if (!showAll) {
            wrapper.eq(CfgRentScheme::getStatus, 1);
        }
        return R.ok(rentSchemeService.list(wrapper));
    }

    @Operation(summary = "查询计租方案详情")
    @GetMapping("/{id}")
    public R<CfgRentScheme> detail(@PathVariable Long id) {
        return R.ok(rentSchemeService.getById(id));
    }

    @Operation(summary = "新增计租方案")
    @PostMapping
    public R<Long> create(@RequestBody CfgRentScheme entity) {
        rentSchemeService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑计租方案")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CfgRentScheme entity) {
        entity.setId(id);
        rentSchemeService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "启用/停用计租方案")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return R.fail("status 参数无效，必须为 0 或 1");
        }
        rentSchemeService.update(new LambdaUpdateWrapper<CfgRentScheme>()
                .eq(CfgRentScheme::getId, id)
                .set(CfgRentScheme::getStatus, status));
        return R.ok(null);
    }

    @Operation(summary = "删除计租方案")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        rentSchemeService.removeById(id);
        return R.ok(null);
    }
}
