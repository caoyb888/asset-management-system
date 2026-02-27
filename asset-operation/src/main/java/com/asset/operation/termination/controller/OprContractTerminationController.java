package com.asset.operation.termination.controller;

import com.asset.common.model.R;
import com.asset.operation.change.dto.ApprovalCallbackDTO;
import com.asset.operation.termination.dto.TerminationCreateDTO;
import com.asset.operation.termination.dto.TerminationDetailVO;
import com.asset.operation.termination.dto.TerminationQueryDTO;
import com.asset.operation.termination.service.OprContractTerminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同解约 Controller
 */
@Tag(name = "05-合同解约管理")
@RestController
@RequestMapping("/opr/terminations")
@RequiredArgsConstructor
public class OprContractTerminationController {

    private final OprContractTerminationService terminationService;

    @Operation(summary = "解约单分页列表")
    @GetMapping
    public R<?> page(TerminationQueryDTO query) {
        return R.ok(terminationService.pageQuery(query));
    }

    @Operation(summary = "解约单详情（含清算明细）")
    @GetMapping("/{id}")
    public R<TerminationDetailVO> getById(@PathVariable Long id) {
        return R.ok(terminationService.getDetailById(id));
    }

    @Operation(summary = "新增解约单（草稿）")
    @PostMapping
    public R<Long> create(@RequestBody TerminationCreateDTO dto) {
        return R.ok(terminationService.create(dto));
    }

    @Operation(summary = "编辑解约单（仅草稿/驳回可改）")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody TerminationCreateDTO dto) {
        terminationService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "计算清算金额（调清算引擎）")
    @PostMapping("/{id}/calculate-settlement")
    public R<?> calculateSettlement(@PathVariable Long id) {
        terminationService.calculateSettlement(id);
        return R.ok();
    }

    @Operation(summary = "提交OA审批")
    @PostMapping("/{id}/submit-approval")
    public R<?> submitApproval(@PathVariable Long id) {
        terminationService.submitApproval(id);
        return R.ok();
    }

    @Operation(summary = "审批回调（解约执行触发点）")
    @PostMapping("/{id}/approval-callback")
    public R<?> approvalCallback(@PathVariable Long id, @RequestBody ApprovalCallbackDTO dto) {
        terminationService.onApprovalCallback(id, dto);
        return R.ok();
    }
}
