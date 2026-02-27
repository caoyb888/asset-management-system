package com.asset.finance.receipt.controller;
import com.asset.common.model.R;
import com.asset.finance.receipt.service.FinReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name = "02-收款管理") @RestController @RequestMapping("/fin/receipts") @RequiredArgsConstructor
public class FinReceiptController {
    private final FinReceiptService receiptService;
    @Operation(summary = "收款单列表") @GetMapping public R<?> page() { return R.ok(); }
    @Operation(summary = "新增收款单") @PostMapping public R<?> create(@RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "收款单详情") @GetMapping("/{id}") public R<?> getById(@PathVariable Long id) { return R.ok(); }
    @Operation(summary = "核销应收") @PostMapping("/{id}/write-off") public R<?> writeOff(@PathVariable Long id, @RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "作废收款单") @PostMapping("/{id}/void") public R<?> voidReceipt(@PathVariable Long id) { return R.ok(); }
}
