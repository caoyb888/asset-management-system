package com.asset.investment.contract.controller;

import com.asset.common.model.R;
import com.asset.investment.contract.dto.*;
import com.asset.investment.contract.entity.*;
import com.asset.investment.contract.service.InvLeaseContractService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 招商合同 Controller
 *
 * <pre>
 * 状态流转：
 *   草稿(0) ──[发起审批]──► 审批中(1)
 *   审批中(1) ──[通过回调]──► 生效(2)
 *   审批中(1) ──[驳回回调]──► 草稿(0)（重新编辑）
 *   生效(2) ──[到期]──► 到期(3)
 *   生效(2)/到期(3) ──[终止]──► 终止(4)
 * </pre>
 */
@Tag(name = "03-招商合同管理", description = "招商-合同CRUD、状态机与审批")
@RestController
@RequestMapping("/inv/contracts")
@RequiredArgsConstructor
public class InvLeaseContractController {

    private final InvLeaseContractService contractService;

    // ----------------------------------------------------------------
    // 查询
    // ----------------------------------------------------------------

    @Operation(summary = "分页查询合同列表")
    @GetMapping
    public R<IPage<InvLeaseContract>> page(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数，默认20") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "项目ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "状态(0草稿/1审批中/2生效/3到期/4终止)") @RequestParam(required = false) Integer status,
            @Parameter(description = "商家ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "关键词（合同名称/编号）") @RequestParam(required = false) String keyword) {
        ContractQueryDTO query = new ContractQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setProjectId(projectId);
        query.setStatus(status);
        query.setMerchantId(merchantId);
        query.setKeyword(keyword);
        return R.ok(contractService.pageQuery(query));
    }

    @Operation(summary = "查询合同详情")
    @GetMapping("/{id}")
    public R<InvLeaseContract> detail(@PathVariable Long id) {
        return R.ok(contractService.getById(id));
    }

    // ----------------------------------------------------------------
    // 新增 / 编辑 / 删除
    // ----------------------------------------------------------------

    @Operation(summary = "新增合同（草稿）")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ContractSaveDTO dto) {
        return R.ok(contractService.createContract(dto));
    }

    @Operation(summary = "编辑合同（仅草稿状态可修改）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody ContractSaveDTO dto) {
        contractService.updateContract(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除合同（审批中/生效状态不可删）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contractService.deleteContract(id);
        return R.ok(null);
    }

    // ----------------------------------------------------------------
    // 任务 5.1 — 意向转合同
    // ----------------------------------------------------------------

    @Operation(summary = "意向协议转合同（含分布式锁防重复）")
    @PostMapping("/from-intention/{intentionId}")
    public R<Long> fromIntention(@PathVariable Long intentionId,
                                 @Valid @RequestBody ContractSaveDTO dto) {
        return R.ok(contractService.convertFromIntention(intentionId, dto));
    }

    // ----------------------------------------------------------------
    // 状态机操作
    // ----------------------------------------------------------------

    @Operation(summary = "发起审批（草稿 → 审批中）")
    @PostMapping("/{id}/submit-approval")
    public R<Void> submitApproval(@PathVariable Long id) {
        contractService.submitApproval(id);
        return R.ok(null);
    }

    @Operation(summary = "审批回调（审批中 → 生效/草稿）")
    @PostMapping("/{id}/approval-callback")
    public R<Void> approvalCallback(@PathVariable Long id,
                                    @Valid @RequestBody ContractApprovalCallbackDTO dto) {
        contractService.handleApprovalCallback(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "更新合同状态（到期/终止等系统操作）")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id,
                                @RequestParam Integer status) {
        contractService.updateStatus(id, status);
        return R.ok(null);
    }

    // ----------------------------------------------------------------
    // 商铺关联
    // ----------------------------------------------------------------

    @Operation(summary = "批量保存商铺关联（全量替换）")
    @PostMapping("/{id}/shops")
    public R<Void> saveShops(@PathVariable Long id,
                             @RequestBody List<ContractShopItemDTO> shops) {
        contractService.saveShops(id, shops);
        return R.ok(null);
    }

    @Operation(summary = "查询合同关联商铺列表")
    @GetMapping("/{id}/shops")
    public R<List<InvLeaseContractShop>> listShops(@PathVariable Long id) {
        return R.ok(contractService.listShops(id));
    }

    // ----------------------------------------------------------------
    // 费项配置
    // ----------------------------------------------------------------

    @Operation(summary = "批量保存费项配置（全量替换）")
    @PostMapping("/{id}/fees")
    public R<Void> saveFees(@PathVariable Long id,
                            @RequestBody List<ContractFeeItemDTO> fees) {
        contractService.saveFees(id, fees);
        return R.ok(null);
    }

    @Operation(summary = "查询合同费项列表")
    @GetMapping("/{id}/fees")
    public R<List<InvLeaseContractFee>> listFees(@PathVariable Long id) {
        return R.ok(contractService.listFees(id));
    }

    // ----------------------------------------------------------------
    // 分铺计租阶段
    // ----------------------------------------------------------------

    @Operation(summary = "批量保存分铺计租阶段")
    @PostMapping("/{id}/fee-stages")
    public R<Void> saveFeeStages(@PathVariable Long id,
                                 @RequestBody List<ContractFeeStageItemDTO> stages) {
        contractService.saveFeeStages(id, stages);
        return R.ok(null);
    }

    @Operation(summary = "查询分铺计租阶段列表")
    @GetMapping("/{id}/fee-stages")
    public R<List<InvLeaseContractFeeStage>> listFeeStages(@PathVariable Long id) {
        return R.ok(contractService.listFeeStages(id));
    }

    // ----------------------------------------------------------------
    // 费用生成 & 账期生成
    // ----------------------------------------------------------------

    @Operation(summary = "生成费用明细（调用计算引擎，更新 total_amount）")
    @PostMapping("/{id}/generate-cost")
    public R<Map<String, Object>> generateCost(@PathVariable Long id) {
        return R.ok(contractService.generateCost(id));
    }

    @Operation(summary = "生成账期（按支付周期拆分，全量替换）")
    @PostMapping("/{id}/billing")
    public R<List<InvLeaseContractBilling>> generateBilling(@PathVariable Long id) {
        return R.ok(contractService.generateBilling(id));
    }

    @Operation(summary = "查询账期列表")
    @GetMapping("/{id}/billing")
    public R<List<InvLeaseContractBilling>> listBilling(@PathVariable Long id) {
        return R.ok(contractService.listBilling(id));
    }

    // ----------------------------------------------------------------
    // 任务 5.3 — 版本历史
    // ----------------------------------------------------------------

    @Operation(summary = "查询合同版本历史列表（按版本降序）")
    @GetMapping("/{id}/versions")
    public R<List<InvLeaseContractVersion>> listVersions(@PathVariable Long id) {
        return R.ok(contractService.listVersions(id));
    }
}
