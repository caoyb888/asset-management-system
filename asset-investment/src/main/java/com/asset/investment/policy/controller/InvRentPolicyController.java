package com.asset.investment.policy.controller;

import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.investment.policy.entity.InvRentPolicy;
import com.asset.investment.policy.entity.InvRentPolicyIndicator;
import com.asset.investment.policy.service.InvRentPolicyIndicatorService;
import com.asset.investment.policy.service.InvRentPolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 租决政策 Controller
 * 状态流转：草稿(0) → 审批中(1) → 通过(2)/驳回(3)
 */
@Tag(name = "05-租决政策管理", description = "租决政策CRUD、分类指标配置与审批")
@RestController
@RequestMapping("/inv/rent-policies")
@RequiredArgsConstructor
public class InvRentPolicyController {

    private final InvRentPolicyService policyService;
    private final InvRentPolicyIndicatorService indicatorService;

    // ── 查询 ──────────────────────────────────────────────

    @Operation(summary = "分页查询租决政策列表")
    @GetMapping
    public R<IPage<InvRentPolicy>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InvRentPolicy> wrapper = new LambdaQueryWrapper<InvRentPolicy>()
                .eq(projectId != null, InvRentPolicy::getProjectId, projectId)
                .eq(status != null, InvRentPolicy::getStatus, status)
                .orderByDesc(InvRentPolicy::getCreatedAt);
        return R.ok(policyService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<InvRentPolicy> detail(@PathVariable Long id) {
        return R.ok(policyService.getById(id));
    }

    @Operation(summary = "获取已审批通过的政策列表（供租金分解选择）")
    @GetMapping("/approved")
    public R<List<InvRentPolicy>> listApproved(
            @RequestParam(required = false) Long projectId) {
        LambdaQueryWrapper<InvRentPolicy> wrapper = new LambdaQueryWrapper<InvRentPolicy>()
                .eq(InvRentPolicy::getStatus, 2)
                .eq(projectId != null, InvRentPolicy::getProjectId, projectId)
                .orderByDesc(InvRentPolicy::getCreatedAt);
        return R.ok(policyService.list(wrapper));
    }

    // ── 新增 / 编辑 / 删除 ──────────────────────────────────

    @Operation(summary = "新增租决政策（草稿）")
    @PostMapping
    public R<Long> create(@RequestBody InvRentPolicy entity) {
        entity.setStatus(0);
        entity.setPolicyCode(generateCode());
        policyService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑租决政策（仅草稿/驳回状态）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvRentPolicy entity) {
        InvRentPolicy existing = policyService.getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() == 1) throw new BizException("审批中不可修改");
        entity.setId(id);
        policyService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除租决政策")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        InvRentPolicy existing = policyService.getById(id);
        if (existing != null && existing.getStatus() == 1) throw new BizException("审批中不可删除");
        if (existing != null && existing.getStatus() == 2) throw new BizException("已通过的政策不可删除");
        // 级联删除指标
        indicatorService.remove(new LambdaQueryWrapper<InvRentPolicyIndicator>()
                .eq(InvRentPolicyIndicator::getPolicyId, id));
        policyService.removeById(id);
        return R.ok(null);
    }

    // ── 状态机 ──────────────────────────────────────────────

    @Operation(summary = "提交审批（草稿0 → 审批中1）")
    @PostMapping("/{id}/submit-approval")
    public R<Void> submitApproval(@PathVariable Long id) {
        InvRentPolicy existing = policyService.getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 0 && existing.getStatus() != 3)
            throw new BizException("仅草稿或驳回状态可提交审批");
        String mockApprovalId = "MOCK-POLICY-" + id + "-" + System.currentTimeMillis();
        policyService.update(new LambdaUpdateWrapper<InvRentPolicy>()
                .eq(InvRentPolicy::getId, id)
                .set(InvRentPolicy::getStatus, 1)
                .set(InvRentPolicy::getApprovalId, mockApprovalId));
        return R.ok(null);
    }

    @Operation(summary = "审批回调（审批中1 → 通过2/驳回3）")
    @PostMapping("/{id}/approval-callback")
    public R<Void> approvalCallback(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        InvRentPolicy existing = policyService.getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 1) throw new BizException("当前状态不在审批中");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        policyService.update(new LambdaUpdateWrapper<InvRentPolicy>()
                .eq(InvRentPolicy::getId, id)
                .set(InvRentPolicy::getStatus, approved ? 2 : 3));
        return R.ok(null);
    }

    // ── 分类指标管理 ──────────────────────────────────────────

    @Operation(summary = "查询分类指标列表（按商铺类别排序）")
    @GetMapping("/{id}/indicators")
    public R<List<InvRentPolicyIndicator>> listIndicators(@PathVariable Long id) {
        return R.ok(indicatorService.list(new LambdaQueryWrapper<InvRentPolicyIndicator>()
                .eq(InvRentPolicyIndicator::getPolicyId, id)
                .orderByAsc(InvRentPolicyIndicator::getShopCategory)
                .orderByAsc(InvRentPolicyIndicator::getId)));
    }

    @Operation(summary = "批量保存分类指标（全量替换）")
    @PostMapping("/{id}/indicators")
    public R<Void> saveIndicators(@PathVariable Long id,
                                  @RequestBody List<InvRentPolicyIndicator> indicators) {
        InvRentPolicy existing = policyService.getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() == 1) throw new BizException("审批中不可修改指标");
        // 全量替换
        indicatorService.remove(new LambdaQueryWrapper<InvRentPolicyIndicator>()
                .eq(InvRentPolicyIndicator::getPolicyId, id));
        if (indicators != null && !indicators.isEmpty()) {
            indicators.forEach(ind -> ind.setPolicyId(id));
            indicatorService.saveBatch(indicators);
        }
        return R.ok(null);
    }

    // ── 私有工具 ──────────────────────────────────────────────

    private String generateCode() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long count = policyService.count();
        return "RP" + year + String.format("%04d", count + 1);
    }
}
