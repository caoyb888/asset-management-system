package com.asset.finance.voucher.controller;
import com.asset.common.model.R;
import com.asset.finance.voucher.service.FinVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name = "03-凭证管理") @RestController @RequestMapping("/fin/vouchers") @RequiredArgsConstructor
public class FinVoucherController {
    private final FinVoucherService voucherService;
    @Operation(summary = "凭证列表") @GetMapping public R<?> page() { return R.ok(); }
    @Operation(summary = "生成凭证") @PostMapping public R<?> create(@RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "凭证详情") @GetMapping("/{id}") public R<?> getById(@PathVariable Long id) { return R.ok(); }
    @Operation(summary = "审核凭证") @PostMapping("/{id}/audit") public R<?> audit(@PathVariable Long id) { return R.ok(); }
}
