package com.asset.finance.deposit.controller;

import com.asset.common.model.R;
import com.asset.finance.deposit.dto.*;
import com.asset.finance.deposit.entity.FinDepositTransaction;
import com.asset.finance.deposit.service.FinDepositService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 保证金管理 Controller
 */
@Tag(name = "04-保证金管理")
@RestController
@RequestMapping("/fin/deposits")
@RequiredArgsConstructor
public class FinDepositController {

    private final FinDepositService depositService;

    /** 查询合同保证金账户余额卡片 */
    @Operation(summary = "查询保证金账户")
    @GetMapping("/account")
    public R<DepositAccountVO> getAccount(@RequestParam Long contractId) {
        return R.ok(depositService.getAccount(contractId));
    }

    /** 分页查询保证金流水 */
    @Operation(summary = "分页查询保证金流水")
    @GetMapping("/transactions")
    public R<IPage<FinDepositTransaction>> pageTransaction(@Valid DepositQueryDTO query) {
        return R.ok(depositService.pageTransaction(query));
    }

    /** 缴纳保证金（直接生效，无需审批） */
    @Operation(summary = "缴纳保证金")
    @PostMapping("/pay-in")
    public R<Void> payIn(@Valid @RequestBody DepositPayInDTO dto) {
        depositService.payIn(dto);
        return R.ok();
    }

    /** 申请保证金冲抵应收 */
    @Operation(summary = "申请冲抵应收")
    @PostMapping("/offset")
    public R<Long> applyOffset(@Valid @RequestBody DepositOffsetDTO dto) {
        return R.ok(depositService.processOffset(dto));
    }

    /** 申请保证金退款 */
    @Operation(summary = "申请退款")
    @PostMapping("/refund")
    public R<Long> applyRefund(@Valid @RequestBody DepositRefundDTO dto) {
        return R.ok(depositService.processRefund(dto));
    }

    /** 申请保证金罚没 */
    @Operation(summary = "申请罚没")
    @PostMapping("/forfeit")
    public R<Long> applyForfeit(@Valid @RequestBody DepositForfeitDTO dto) {
        return R.ok(depositService.processForfeit(dto));
    }

    /** OA审批回调（通过/驳回） */
    @Operation(summary = "OA审批回调")
    @PostMapping("/approval-callback")
    public R<Void> approvalCallback(@RequestBody ApprovalCallbackRequest req) {
        depositService.approveCallback(req.getApprovalId(), req.isApproved());
        return R.ok();
    }

    /** 审批回调请求体 */
    @Data
    static class ApprovalCallbackRequest {
        private String approvalId;
        private boolean approved;
    }
}
