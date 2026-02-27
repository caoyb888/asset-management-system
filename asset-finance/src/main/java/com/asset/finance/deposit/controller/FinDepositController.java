package com.asset.finance.deposit.controller;
import com.asset.common.model.R;
import com.asset.finance.deposit.service.FinDepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name = "04-保证金管理") @RestController @RequestMapping("/fin/deposits") @RequiredArgsConstructor
public class FinDepositController {
    private final FinDepositService depositService;
    @Operation(summary = "保证金列表") @GetMapping public R<?> page() { return R.ok(); }
    @Operation(summary = "保证金详情") @GetMapping("/{id}") public R<?> getById(@PathVariable Long id) { return R.ok(); }
    @Operation(summary = "缴纳保证金") @PostMapping("/{id}/pay-in") public R<?> payIn(@PathVariable Long id, @RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "申请冲抵应收") @PostMapping("/{id}/deduct") public R<?> deduct(@PathVariable Long id, @RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "申请退款") @PostMapping("/{id}/refund") public R<?> refund(@PathVariable Long id, @RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "保证金流水") @GetMapping("/{id}/records") public R<?> records(@PathVariable Long id) { return R.ok(); }
}
