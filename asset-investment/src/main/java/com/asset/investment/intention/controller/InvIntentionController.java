package com.asset.investment.intention.controller;

import com.asset.common.model.R;
import com.asset.investment.intention.dto.ApprovalCallbackDTO;
import com.asset.investment.intention.dto.IntentionQueryDTO;
import com.asset.investment.intention.dto.IntentionSaveDTO;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.service.InvIntentionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 意向协议 Controller
 * 对应任务 4.1（CRUD + 状态机）和任务 4.3（审批回调）
 *
 * <pre>
 * 状态流转：
 *   草稿(0) ──[发起审批]──► 审批中(1)
 *   审批中(1) ──[通过回调]──► 审批通过(2)
 *   审批中(1) ──[驳回回调]──► 驳回(3)
 *   驳回(3) ──[重新发起]──► 审批中(1)
 *   审批通过(2) ──[转合同]──► 已转合同(4)（见任务5.1）
 * </pre>
 */
@Tag(name = "02-意向协议管理", description = "招商-意向协议CRUD与审批状态机")
@RestController
@RequestMapping("/inv/intentions")
@RequiredArgsConstructor
public class InvIntentionController {

    private final InvIntentionService intentionService;

    // ----------------------------------------------------------------
    // 查询
    // ----------------------------------------------------------------

    /**
     * 分页查询（IA-01）
     * 支持项目/状态/商家/品牌/楼栋/楼层/商铺/业态/关键词等多条件筛选
     * 利用 idx_intention_multi 复合索引加速
     */
    @Operation(summary = "分页查询意向协议列表")
    @GetMapping
    public R<IPage<InvIntention>> page(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数，默认20") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "项目ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "状态(0草稿/1审批中/2通过/3驳回/4已转合同)") @RequestParam(required = false) Integer status,
            @Parameter(description = "商家ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "品牌ID") @RequestParam(required = false) Long brandId,
            @Parameter(description = "楼栋ID") @RequestParam(required = false) Long buildingId,
            @Parameter(description = "楼层ID") @RequestParam(required = false) Long floorId,
            @Parameter(description = "商铺ID") @RequestParam(required = false) Long shopId,
            @Parameter(description = "业态") @RequestParam(required = false) String formatType,
            @Parameter(description = "关键词（意向名称/编号模糊搜索）") @RequestParam(required = false) String keyword) {

        IntentionQueryDTO query = new IntentionQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setProjectId(projectId);
        query.setStatus(status);
        query.setMerchantId(merchantId);
        query.setBrandId(brandId);
        query.setBuildingId(buildingId);
        query.setFloorId(floorId);
        query.setShopId(shopId);
        query.setFormatType(formatType);
        query.setKeyword(keyword);

        return R.ok(intentionService.pageQuery(query));
    }

    /**
     * 查询意向协议详情（IA-02）
     */
    @Operation(summary = "查询意向协议详情")
    @GetMapping("/{id}")
    public R<InvIntention> detail(@PathVariable Long id) {
        return R.ok(intentionService.getById(id));
    }

    // ----------------------------------------------------------------
    // 新增 / 编辑 / 删除
    // ----------------------------------------------------------------

    /**
     * 新增意向协议（IA-03）
     * 状态自动置为草稿(0)，系统生成编号
     */
    @Operation(summary = "新增意向协议（草稿）")
    @PostMapping
    public R<Long> create(@Valid @RequestBody IntentionSaveDTO dto) {
        return R.ok(intentionService.createIntention(dto));
    }

    /**
     * 编辑意向协议（IA-04）
     * 仅草稿(0)/驳回(3)状态可修改
     */
    @Operation(summary = "编辑意向协议（仅草稿/驳回状态可修改）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody IntentionSaveDTO dto) {
        intentionService.updateIntention(id, dto);
        return R.ok(null);
    }

    /**
     * 逻辑删除意向协议（IA-05）
     * 审批中(1)和已转合同(4)不可删除
     */
    @Operation(summary = "删除意向协议（审批中/已转合同不可删）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        intentionService.deleteIntention(id);
        return R.ok(null);
    }

    // ----------------------------------------------------------------
    // 状态机操作
    // ----------------------------------------------------------------

    /**
     * 暂存（IA-10）
     * 仅草稿(0)/驳回(3)状态可暂存
     */
    @Operation(summary = "暂存意向协议（草稿阶段显式保存）")
    @PutMapping("/{id}/draft")
    public R<Void> saveDraft(@PathVariable Long id,
                             @Valid @RequestBody IntentionSaveDTO dto) {
        intentionService.saveDraft(id, dto);
        return R.ok(null);
    }

    /**
     * 发起审批（IA-06）
     * 草稿(0) → 审批中(1)
     * 驳回(3) → 审批中(1)（重新发起）
     */
    @Operation(summary = "发起审批（草稿/驳回 → 审批中）")
    @PostMapping("/{id}/submit-approval")
    public R<Void> submitApproval(@PathVariable Long id) {
        intentionService.submitApproval(id);
        return R.ok(null);
    }

    /**
     * 审批回调（IA-07，对应任务 4.3）
     * 审批中(1) → 审批通过(2) 或 驳回(3)
     * 生产环境由审批引擎回调；当前阶段支持前端 Mock 调用
     */
    @Operation(summary = "审批回调（审批中 → 通过/驳回）")
    @PostMapping("/{id}/approval-callback")
    public R<Void> approvalCallback(@PathVariable Long id,
                                    @Valid @RequestBody ApprovalCallbackDTO dto) {
        intentionService.handleApprovalCallback(id, dto);
        return R.ok(null);
    }
}
