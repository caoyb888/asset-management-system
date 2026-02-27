package com.asset.operation.ledger.controller;

import com.asset.common.model.R;
import com.asset.operation.ledger.dto.AuditDTO;
import com.asset.operation.ledger.dto.LedgerDetailVO;
import com.asset.operation.ledger.dto.LedgerQueryDTO;
import com.asset.operation.ledger.dto.LedgerSelectorVO;
import com.asset.operation.ledger.dto.OneTimePaymentDTO;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.service.OprContractLedgerService;
import com.asset.operation.ledger.service.OprReceivablePlanService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同台账 Controller
 * 阶段一（第1-2周）：台账 CRUD + 双签 + 应收生成 + 审核 + 推送 + 一次性首款
 */
@Tag(name = "01-合同台账管理")
@RestController
@RequestMapping("/opr/ledgers")
@RequiredArgsConstructor
public class OprContractLedgerController {

    private final OprContractLedgerService ledgerService;
    private final OprReceivablePlanService receivablePlanService;

    /** 台账分页列表 */
    @Operation(summary = "台账分页列表查询")
    @GetMapping
    public R<IPage<OprContractLedger>> page(LedgerQueryDTO query) {
        return R.ok(ledgerService.pageQuery(query));
    }

    /** 选择器搜索（供前端 ContractSelector 下拉组件使用） */
    @Operation(summary = "台账选择器模糊搜索（按台账编号/合同编号/商家名）")
    @GetMapping("/search")
    public R<List<LedgerSelectorVO>> search(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "返回条数，默认10，最大50") @RequestParam(defaultValue = "10") int pageSize
    ) {
        return R.ok(ledgerService.searchForSelector(keyword, pageSize));
    }

    /** 台账详情（含应收计划/关联信息） */
    @Operation(summary = "台账详情（含应收计划/关联信息）")
    @GetMapping("/{id}")
    public R<LedgerDetailVO> getById(@PathVariable @Parameter(description = "台账ID") Long id) {
        return R.ok(ledgerService.getDetailById(id));
    }

    /**
     * 根据招商合同创建台账（供内部调用 / 测试手动触发）
     * 生产环境建议通过 MQ 事件驱动
     */
    @Operation(summary = "根据招商合同创建台账")
    @PostMapping("/from-contract/{contractId}")
    public R<Long> fromContract(@PathVariable @Parameter(description = "招商合同ID") Long contractId) {
        Long ledgerId = ledgerService.generateFromContract(contractId);
        return R.ok(ledgerId);
    }

    /** 双签确认 */
    @Operation(summary = "双签确认")
    @PutMapping("/{id}/double-sign")
    public R<Void> doubleSign(@PathVariable @Parameter(description = "台账ID") Long id) {
        ledgerService.confirmDoubleSign(id);
        return R.ok(null);
    }

    /** 生成应收计划 */
    @Operation(summary = "生成应收计划（从招商合同账期）")
    @PostMapping("/{id}/generate-receivable")
    public R<Integer> generateReceivable(@PathVariable @Parameter(description = "台账ID") Long id) {
        int count = ledgerService.generateReceivable(id);
        return R.ok(count);
    }

    /** 审核台账（通过/驳回） */
    @Operation(summary = "审核台账（通过/驳回）")
    @PutMapping("/{id}/audit")
    public R<Void> audit(@PathVariable @Parameter(description = "台账ID") Long id,
                         @RequestBody AuditDTO dto) {
        ledgerService.audit(id, dto);
        return R.ok(null);
    }

    /** 手动推送应收至财务系统 */
    @Operation(summary = "手动推送应收至财务系统")
    @PostMapping("/{id}/push-receivable")
    public R<Void> pushReceivable(@PathVariable @Parameter(description = "台账ID") Long id) {
        ledgerService.pushReceivable(id);
        return R.ok(null);
    }

    /** 一次性首款录入 */
    @Operation(summary = "一次性首款录入")
    @PostMapping("/{id}/one-time-payment")
    public R<Void> addOneTimePayment(@PathVariable @Parameter(description = "台账ID") Long id,
                                     @RequestBody OneTimePaymentDTO dto) {
        ledgerService.addOneTimePayment(id, dto);
        return R.ok(null);
    }

    /** 查询台账下应收计划列表 */
    @Operation(summary = "查询应收计划列表")
    @GetMapping("/{id}/receivables")
    public R<?> listReceivables(@PathVariable @Parameter(description = "台账ID") Long id) {
        return R.ok(receivablePlanService.listByLedgerId(id));
    }
}
