package com.asset.operation.flow.controller;

import com.asset.common.model.R;
import com.asset.operation.flow.dto.PassengerFlowCreateDTO;
import com.asset.operation.flow.dto.PassengerFlowQueryDTO;
import com.asset.operation.flow.service.OprPassengerFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客流填报 Controller
 */
@Tag(name = "04-客流填报管理")
@RestController
@RequestMapping("/opr/passenger-flows")
@RequiredArgsConstructor
public class OprPassengerFlowController {

    private final OprPassengerFlowService passengerFlowService;

    @Operation(summary = "客流分页列表")
    @GetMapping
    public R<?> page(PassengerFlowQueryDTO query) {
        return R.ok(passengerFlowService.pageQuery(query));
    }

    @Operation(summary = "新增客流填报")
    @PostMapping
    public R<Long> create(@RequestBody PassengerFlowCreateDTO dto) {
        return R.ok(passengerFlowService.create(dto));
    }

    @Operation(summary = "编辑客流填报")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody PassengerFlowCreateDTO dto) {
        passengerFlowService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除客流填报")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        passengerFlowService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量导入客流（Excel）")
    @PostMapping("/import")
    public R<?> importExcel(@RequestParam("file") MultipartFile file) {
        return R.ok(passengerFlowService.importExcel(file));
    }

    @Operation(summary = "导出客流报表（Excel）")
    @GetMapping("/export")
    public void exportExcel(PassengerFlowQueryDTO query, HttpServletResponse response) {
        passengerFlowService.exportExcel(query, response);
    }

    @Operation(summary = "客流统计分析（日/周环比 + 近30天趋势）")
    @GetMapping("/statistics")
    public R<?> statistics(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId) {
        return R.ok(passengerFlowService.statistics(projectId, buildingId, floorId));
    }
}
