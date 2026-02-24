package com.asset.investment.opening.controller;

import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.investment.opening.entity.InvOpeningApproval;
import com.asset.investment.opening.entity.InvOpeningAttachment;
import com.asset.investment.opening.service.InvOpeningApprovalService;
import com.asset.investment.opening.service.InvOpeningAttachmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 开业审批 Controller
 * 状态流转：草稿(0) → 审批中(1) → 通过(2)/驳回(3)
 */
@Tag(name = "04-开业审批管理", description = "招商-开业审批CRUD与审批流程")
@RestController
@RequestMapping("/inv/opening-approvals")
@RequiredArgsConstructor
public class InvOpeningApprovalController {

    private final InvOpeningApprovalService approvalService;
    private final InvOpeningAttachmentService attachmentService;

    // ── 查询 ──────────────────────────────────────────────

    @Operation(summary = "分页查询开业审批列表")
    @GetMapping
    public R<IPage<InvOpeningApproval>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long contractId) {
        LambdaQueryWrapper<InvOpeningApproval> wrapper = new LambdaQueryWrapper<InvOpeningApproval>()
                .eq(projectId != null, InvOpeningApproval::getProjectId, projectId)
                .eq(status != null, InvOpeningApproval::getStatus, status)
                .eq(contractId != null, InvOpeningApproval::getContractId, contractId)
                .orderByDesc(InvOpeningApproval::getCreatedAt);
        return R.ok(approvalService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<InvOpeningApproval> detail(@PathVariable Long id) {
        return R.ok(approvalService.getById(id));
    }

    // ── 新增 / 编辑 / 删除 ──────────────────────────────────

    @Operation(summary = "新增开业审批（草稿）")
    @PostMapping
    public R<Long> create(@RequestBody InvOpeningApproval entity) {
        entity.setStatus(0);
        entity.setApprovalCode(generateCode());
        approvalService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑开业审批（仅草稿/驳回状态可修改）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvOpeningApproval entity) {
        InvOpeningApproval existing = approvalService.getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() == 1) throw new BizException("审批中不可修改");
        entity.setId(id);
        approvalService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除开业审批（审批中/通过状态不可删）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        InvOpeningApproval existing = approvalService.getById(id);
        if (existing != null && existing.getStatus() == 1) throw new BizException("审批中不可删除");
        if (existing != null && existing.getStatus() == 2) throw new BizException("已通过的记录不可删除");
        approvalService.removeById(id);
        return R.ok(null);
    }

    // ── 状态机 ──────────────────────────────────────────────

    @Operation(summary = "提交审批（草稿0 → 审批中1）")
    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        InvOpeningApproval existing = approvalService.getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 0) throw new BizException("仅草稿状态可提交审批");
        String mockApprovalId = "MOCK-OPENING-" + id + "-" + System.currentTimeMillis();
        approvalService.update(new LambdaUpdateWrapper<InvOpeningApproval>()
                .eq(InvOpeningApproval::getId, id)
                .set(InvOpeningApproval::getStatus, 1)
                .set(InvOpeningApproval::getApprovalId, mockApprovalId));
        return R.ok(null);
    }

    @Operation(summary = "审批回调（审批中1 → 通过2/驳回3）")
    @PostMapping("/{id}/approval-callback")
    public R<Void> approvalCallback(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        InvOpeningApproval existing = approvalService.getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 1) throw new BizException("当前状态不在审批中");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        approvalService.update(new LambdaUpdateWrapper<InvOpeningApproval>()
                .eq(InvOpeningApproval::getId, id)
                .set(InvOpeningApproval::getStatus, approved ? 2 : 3));
        return R.ok(null);
    }

    // ── 附件管理 ──────────────────────────────────────────────

    @Operation(summary = "查询附件列表")
    @GetMapping("/{id}/attachments")
    public R<List<InvOpeningAttachment>> listAttachments(@PathVariable Long id) {
        return R.ok(attachmentService.list(new LambdaQueryWrapper<InvOpeningAttachment>()
                .eq(InvOpeningAttachment::getOpeningApprovalId, id)
                .orderByAsc(InvOpeningAttachment::getId)));
    }

    @Operation(summary = "新增附件记录（保存文件URL）")
    @PostMapping("/{id}/attachments")
    public R<Long> addAttachment(@PathVariable Long id,
                                 @RequestBody InvOpeningAttachment attachment) {
        attachment.setOpeningApprovalId(id);
        attachmentService.save(attachment);
        return R.ok(attachment.getId());
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/attachments/{attachmentId}")
    public R<Void> deleteAttachment(@PathVariable Long attachmentId) {
        attachmentService.removeById(attachmentId);
        return R.ok(null);
    }

    // ── 私有工具 ──────────────────────────────────────────────

    private String generateCode() {
        long count = approvalService.count();
        return "OA" + String.format("%06d", count + 1);
    }
}
