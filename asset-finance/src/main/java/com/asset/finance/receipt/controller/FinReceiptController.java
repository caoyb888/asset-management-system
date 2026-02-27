package com.asset.finance.receipt.controller;

import com.asset.common.model.R;
import com.asset.finance.receipt.dto.ReceiptCreateDTO;
import com.asset.finance.receipt.dto.ReceiptQueryDTO;
import com.asset.finance.receipt.service.FinReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 收款管理接口
 */
@Tag(name = "02-收款管理")
@RestController
@RequestMapping("/fin/receipts")
@RequiredArgsConstructor
public class FinReceiptController {

    private final FinReceiptService receiptService;

    @Operation(summary = "收款单分页列表")
    @GetMapping
    public R<?> page(ReceiptQueryDTO query) {
        return R.ok(receiptService.pageQuery(query));
    }

    @Operation(summary = "收款单详情（含拆分明细）")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(receiptService.getDetailById(id));
    }

    @Operation(summary = "新增收款单")
    @PostMapping
    public R<?> create(@Valid @RequestBody ReceiptCreateDTO dto) {
        return R.ok(receiptService.create(dto));
    }

    @Operation(summary = "编辑收款单（仅待核销状态可编辑）")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody ReceiptCreateDTO dto) {
        receiptService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "作废收款单（仅待核销状态可作废）")
    @PutMapping("/{id}/cancel")
    public R<?> cancel(
            @PathVariable Long id,
            @Parameter(description = "作废原因") @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        receiptService.cancel(id, reason);
        return R.ok();
    }

    @Operation(summary = "未名款项归名（绑定合同）")
    @PutMapping("/{id}/bind")
    public R<?> bind(
            @PathVariable Long id,
            @Parameter(description = "合同ID") @RequestBody Map<String, Long> body) {
        receiptService.bind(id, body.get("contractId"));
        return R.ok();
    }
}
