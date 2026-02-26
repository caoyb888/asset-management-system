package com.asset.operation.change.controller;

import com.asset.common.model.R;
import com.asset.operation.change.dto.*;
import com.asset.operation.change.entity.OprContractChange;
import com.asset.operation.change.service.OprContractChangeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同变更 Controller
 * 阶段二（第3周）：变更 CRUD / 影响预览 / 审批提交 / 历史查询
 */
@Tag(name = "02-合同变更管理")
@RestController
@RequestMapping("/opr/contract-changes")
@RequiredArgsConstructor
public class OprContractChangeController {

    private final OprContractChangeService changeService;

    /** 变更单分页列表 */
    @Operation(summary = "变更单分页列表")
    @GetMapping
    public R<IPage<OprContractChange>> page(ChangeQueryDTO query) {
        return R.ok(changeService.pageQuery(query));
    }

    /** 变更单详情（含变更类型/字段明细） */
    @Operation(summary = "变更单详情（含明细/快照）")
    @GetMapping("/{id}")
    public R<ChangeDetailVO> getById(@PathVariable @Parameter(description = "变更单ID") Long id) {
        return R.ok(changeService.getDetailById(id));
    }

    /** 新增变更单 */
    @Operation(summary = "新增变更单（草稿）")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ChangeCreateDTO dto) {
        return R.ok(changeService.create(dto));
    }

    /** 编辑变更单（仅草稿/驳回可改） */
    @Operation(summary = "编辑变更单（仅草稿/驳回状态）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable @Parameter(description = "变更单ID") Long id,
                          @RequestBody @Valid ChangeCreateDTO dto) {
        changeService.update(id, dto);
        return R.ok(null);
    }

    /** 预览变更影响（受影响应收笔数/金额差异） */
    @Operation(summary = "预览变更影响（应收笔数/金额差异），结果暂存")
    @PostMapping("/{id}/preview-impact")
    public R<ChangeImpactVO> previewImpact(@PathVariable @Parameter(description = "变更单ID") Long id) {
        return R.ok(changeService.previewImpact(id));
    }

    /** 提交 OA 审批 */
    @Operation(summary = "提交OA审批")
    @PostMapping("/{id}/submit-approval")
    public R<Void> submitApproval(@PathVariable @Parameter(description = "变更单ID") Long id) {
        changeService.submitApproval(id);
        return R.ok(null);
    }

    /** 审批回调（OA 系统主动回调，通过后触发应收重算） */
    @Operation(summary = "审批回调（通过→应收重算，驳回→状态回退）")
    @PostMapping("/{id}/approval-callback")
    public R<Void> approvalCallback(@PathVariable @Parameter(description = "变更单ID") Long id,
                                    @RequestBody ApprovalCallbackDTO dto) {
        changeService.onApprovalCallback(id, dto);
        return R.ok(null);
    }

    /** 合同变更历史时间线 */
    @Operation(summary = "合同变更历史时间线（按合同ID倒序）")
    @GetMapping("/history/{contractId}")
    public R<List<ChangeDetailVO>> history(
            @PathVariable @Parameter(description = "招商合同ID") Long contractId) {
        return R.ok(changeService.listHistory(contractId));
    }
}
