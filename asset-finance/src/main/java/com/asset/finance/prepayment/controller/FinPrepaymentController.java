package com.asset.finance.prepayment.controller;
import com.asset.common.model.R;
import com.asset.finance.prepayment.service.FinPrepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name = "05-预收款管理") @RestController @RequestMapping("/fin/prepayments") @RequiredArgsConstructor
public class FinPrepaymentController {
    private final FinPrepaymentService prepaymentService;
    @Operation(summary = "预收款列表") @GetMapping public R<?> page() { return R.ok(); }
    @Operation(summary = "预收款详情") @GetMapping("/{id}") public R<?> getById(@PathVariable Long id) { return R.ok(); }
    @Operation(summary = "录入预收款") @PostMapping public R<?> create(@RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "抵冲应收") @PostMapping("/{id}/deduct") public R<?> deduct(@PathVariable Long id, @RequestBody Object dto) { return R.ok(); }
    @Operation(summary = "申请退款") @PostMapping("/{id}/refund") public R<?> refund(@PathVariable Long id, @RequestBody Object dto) { return R.ok(); }
}
