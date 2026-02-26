package com.asset.operation.termination.controller;

import com.asset.common.model.R;
import com.asset.operation.termination.service.OprContractTerminationService;
import com.asset.operation.termination.service.OprTerminationSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同解约 Controller
 * 阶段五（第5周后半-第6周）实现：解约CRUD/清算计算/审批/事务性执行
 */
@Tag(name = "05-合同解约管理")
@RestController
@RequestMapping("/opr/terminations")
@RequiredArgsConstructor
public class OprContractTerminationController {

    private final OprContractTerminationService terminationService;
    private final OprTerminationSettlementService settlementService;

    @Operation(summary = "解约单列表查询")
    @GetMapping
    public R<?> page() {
        // TODO 阶段五实现
        return R.ok();
    }

    @Operation(summary = "解约单详情（含清算明细/审批流程）")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        // TODO 阶段五实现
        return R.ok();
    }

    @Operation(summary = "新增解约单（草稿）")
    @PostMapping
    public R<?> create(@RequestBody Object dto) {
        // TODO 阶段五实现
        return R.ok();
    }

    @Operation(summary = "编辑解约单（仅草稿/驳回可改）")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody Object dto) {
        // TODO 阶段五实现
        return R.ok();
    }

    @Operation(summary = "计算清算金额（调清算引擎）")
    @PostMapping("/{id}/calculate-settlement")
    public R<?> calculateSettlement(@PathVariable Long id) {
        // TODO 阶段五实现
        return R.ok();
    }

    @Operation(summary = "提交OA审批")
    @PostMapping("/{id}/submit-approval")
    public R<?> submitApproval(@PathVariable Long id) {
        // TODO 阶段五实现
        return R.ok();
    }

    @Operation(summary = "审批回调（解约执行触发点）")
    @PostMapping("/{id}/approval-callback")
    public R<?> approvalCallback(@PathVariable Long id, @RequestBody Object dto) {
        // TODO 阶段五实现
        return R.ok();
    }

    @Operation(summary = "执行解约（事务性多表联动）")
    @PostMapping("/{id}/execute")
    public R<?> execute(@PathVariable Long id) {
        // TODO 阶段五实现 - @Transactional 多表原子更新
        return R.ok();
    }
}
