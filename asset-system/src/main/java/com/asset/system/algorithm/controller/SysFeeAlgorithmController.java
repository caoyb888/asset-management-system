package com.asset.system.algorithm.controller;

import com.asset.common.model.R;
import com.asset.system.algorithm.dto.CalcTestDTO;
import com.asset.system.algorithm.dto.FeeAlgorithmCreateDTO;
import com.asset.system.algorithm.dto.FeeAlgorithmQueryDTO;
import com.asset.system.algorithm.service.SysFeeAlgorithmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 租费算法规则接口 */
@Tag(name = "09-租费算法管理")
@RestController
@RequestMapping("/sys/fee-algorithms")
@RequiredArgsConstructor
public class SysFeeAlgorithmController {

    private final SysFeeAlgorithmService algorithmService;

    @Operation(summary = "分页查询算法列表")
    @GetMapping
    public R<?> page(FeeAlgorithmQueryDTO query) {
        return R.ok(algorithmService.pageQuery(query));
    }

    @Operation(summary = "查询已启用算法列表（下拉用）")
    @GetMapping("/enabled")
    public R<?> listEnabled() {
        return R.ok(algorithmService.listEnabled());
    }

    @Operation(summary = "新增算法")
    @PostMapping
    public R<?> create(@Valid @RequestBody FeeAlgorithmCreateDTO dto) {
        return R.ok(algorithmService.createAlgorithm(dto));
    }

    @Operation(summary = "更新算法")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody FeeAlgorithmCreateDTO dto) {
        dto.setId(id);
        algorithmService.updateAlgorithm(dto);
        return R.ok();
    }

    @Operation(summary = "删除算法")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        algorithmService.deleteAlgorithm(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用算法")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        algorithmService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "服务端试算（代入变量值，返回结果和展开公式）")
    @PostMapping("/test-calc")
    public R<?> testCalc(@Valid @RequestBody CalcTestDTO dto) {
        return R.ok(algorithmService.testCalc(dto));
    }
}
