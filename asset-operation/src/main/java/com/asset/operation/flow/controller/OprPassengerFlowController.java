package com.asset.operation.flow.controller;

import com.asset.common.model.R;
import com.asset.operation.flow.service.OprPassengerFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客流填报 Controller
 * 阶段四（第5周前半）实现：客流CRUD/导入导出/趋势统计
 */
@Tag(name = "04-客流填报管理")
@RestController
@RequestMapping("/opr/passenger-flows")
@RequiredArgsConstructor
public class OprPassengerFlowController {

    private final OprPassengerFlowService passengerFlowService;

    @Operation(summary = "客流列表查询")
    @GetMapping
    public R<?> page() {
        // TODO 阶段四实现
        return R.ok();
    }

    @Operation(summary = "新增客流填报")
    @PostMapping
    public R<?> create(@RequestBody Object dto) {
        // TODO 阶段四实现
        return R.ok();
    }

    @Operation(summary = "批量导入客流（Excel）")
    @PostMapping("/import")
    public R<?> importExcel(@RequestParam("file") MultipartFile file) {
        // TODO 阶段四实现
        return R.ok();
    }

    @Operation(summary = "导出客流报表（Excel）")
    @GetMapping("/export")
    public void exportExcel() {
        // TODO 阶段四实现
    }

    @Operation(summary = "客流趋势统计（日/周环比）")
    @GetMapping("/statistics")
    public R<?> statistics() {
        // TODO 阶段四实现
        return R.ok();
    }
}
