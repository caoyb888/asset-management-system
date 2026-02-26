package com.asset.operation.ledger.controller;

import com.asset.common.model.R;
import com.asset.operation.ledger.service.OprContractLedgerService;
import com.asset.operation.ledger.service.OprReceivablePlanService;
import com.asset.operation.ledger.service.OprOneTimePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同台账 Controller
 * 阶段一（第1-2周）实现：台账查询/双签/审核/应收生成/推送/一次性首款
 */
@Tag(name = "01-合同台账管理")
@RestController
@RequestMapping("/opr/ledgers")
@RequiredArgsConstructor
public class OprContractLedgerController {

    private final OprContractLedgerService ledgerService;
    private final OprReceivablePlanService receivablePlanService;
    private final OprOneTimePaymentService oneTimePaymentService;

    @Operation(summary = "台账分页列表")
    @GetMapping
    public R<?> page() {
        // TODO 阶段一实现
        return R.ok();
    }

    @Operation(summary = "台账详情（含应收计划/双签/审核）")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        // TODO 阶段一实现
        return R.ok();
    }

    @Operation(summary = "双签确认")
    @PutMapping("/{id}/double-sign")
    public R<?> doubleSign(@PathVariable Long id) {
        // TODO 阶段一实现
        return R.ok();
    }

    @Operation(summary = "生成应收计划")
    @PostMapping("/{id}/generate-receivable")
    public R<?> generateReceivable(@PathVariable Long id) {
        // TODO 阶段一实现
        return R.ok();
    }

    @Operation(summary = "审核台账（通过/驳回）")
    @PutMapping("/{id}/audit")
    public R<?> audit(@PathVariable Long id) {
        // TODO 阶段一实现
        return R.ok();
    }

    @Operation(summary = "手动推送应收至财务系统")
    @PostMapping("/{id}/push-receivable")
    public R<?> pushReceivable(@PathVariable Long id) {
        // TODO 阶段一实现
        return R.ok();
    }

    @Operation(summary = "一次性首款录入")
    @PostMapping("/{id}/one-time-payment")
    public R<?> addOneTimePayment(@PathVariable Long id, @RequestBody Object dto) {
        // TODO 阶段一实现
        return R.ok();
    }

    @Operation(summary = "查询应收计划列表")
    @GetMapping("/{id}/receivables")
    public R<?> listReceivables(@PathVariable Long id) {
        // TODO 阶段一实现
        return R.ok();
    }
}
