package com.asset.finance.reduction.controller;
import com.asset.common.model.R;
import com.asset.finance.reduction.service.FinReductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name = "06-减免调整") @RestController @RequestMapping("/fin/reductions") @RequiredArgsConstructor
public class FinReductionController {
    private final FinReductionService reductionService;
    @Operation(summary = "减免记录列表") @GetMapping public R<?> page() { return R.ok(); }
    @Operation(summary = "新增减免申请") @PostMapping public R<?> create(@RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "减免详情") @GetMapping("/{id}") public R<?> getById(@PathVariable Long id) { return R.ok(); }
    @Operation(summary = "提交审批") @PostMapping("/{id}/submit-approval") public R<?> submitApproval(@PathVariable Long id) { return R.ok(); }
    @Operation(summary = "审批回调") @PostMapping("/{id}/approval-callback") public R<?> approvalCallback(@PathVariable Long id, @RequestBody Object dto) { return R.ok(); }
}
