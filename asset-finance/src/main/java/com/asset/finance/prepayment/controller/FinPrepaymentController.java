package com.asset.finance.prepayment.controller;

import com.asset.common.model.R;
import com.asset.finance.prepayment.dto.*;
import com.asset.finance.prepayment.entity.FinPrepayTransaction;
import com.asset.finance.prepayment.service.FinPrepaymentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预收款管理 Controller
 */
@Tag(name = "05-预收款管理")
@RestController
@RequestMapping("/fin/prepayments")
@RequiredArgsConstructor
public class FinPrepaymentController {

    private final FinPrepaymentService prepaymentService;

    /** 查询合同预收款账户余额 */
    @Operation(summary = "查询预收款账户")
    @GetMapping("/account")
    public R<PrepayAccountVO> getAccount(@RequestParam Long contractId) {
        return R.ok(prepaymentService.getAccount(contractId));
    }

    /** 分页查询预收款流水 */
    @Operation(summary = "分页查询预收款流水")
    @GetMapping("/transactions")
    public R<IPage<FinPrepayTransaction>> pageTransaction(@Valid PrepayQueryDTO query) {
        return R.ok(prepaymentService.pageTransaction(query));
    }

    /** 手动录入预收款（直接生效） */
    @Operation(summary = "录入预收款")
    @PostMapping("/deposit")
    public R<Void> deposit(@Valid @RequestBody PrepayDepositDTO dto) {
        prepaymentService.deposit(dto);
        return R.ok();
    }

    /** 预收款抵冲应收（直接生效） */
    @Operation(summary = "抵冲应收")
    @PostMapping("/offset")
    public R<Void> offset(@Valid @RequestBody PrepayOffsetDTO dto) {
        prepaymentService.offset(dto);
        return R.ok();
    }

    /** 预收款退款（直接生效） */
    @Operation(summary = "退款")
    @PostMapping("/refund")
    public R<Void> refund(@Valid @RequestBody PrepayRefundDTO dto) {
        prepaymentService.refund(dto);
        return R.ok();
    }
}
