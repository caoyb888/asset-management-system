package com.asset.finance.receipt.controller;

import com.asset.common.model.R;
import com.asset.finance.receipt.dto.*;
import com.asset.finance.receipt.service.FinWriteOffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 核销管理 Controller
 */
@Tag(name = "02-核销管理")
@RestController
@RequiredArgsConstructor
public class FinWriteOffController {

    private final FinWriteOffService writeOffService;

    /**
     * 分页查询核销单列表
     */
    @Operation(summary = "分页查询核销单列表")
    @GetMapping("/fin/write-offs")
    public R<?> pageQuery(WriteOffQueryDTO query) {
        return R.ok(writeOffService.pageQuery(query));
    }

    /**
     * 查询可核销的应收记录（按合同ID）
     */
    @Operation(summary = "查询可核销应收记录")
    @GetMapping("/fin/write-offs/writable-receivables")
    public R<List<WritableReceivableVO>> queryWritableReceivables(@RequestParam Long contractId) {
        return R.ok(writeOffService.queryWritableReceivables(contractId));
    }

    /**
     * 提交核销申请
     * Body: { receiptId, writeOffType, items: [...] }
     */
    @Operation(summary = "提交核销申请")
    @PostMapping("/fin/write-offs")
    public R<Long> submitWriteOff(@RequestBody @Valid SubmitWriteOffRequest req) {
        Long id = writeOffService.submitWriteOff(req.getReceiptId(), req.getItems(), req.getWriteOffType());
        return R.ok(id);
    }

    /**
     * OA审批回调
     * Body: { approvalId, approved, comment }
     */
    @Operation(summary = "OA审批回调")
    @PostMapping("/fin/write-offs/approval-callback")
    public R<Void> approveCallback(@RequestBody Map<String, Object> body) {
        String approvalId = (String) body.get("approvalId");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String comment = (String) body.getOrDefault("comment", "");
        writeOffService.approveCallback(approvalId, approved, comment);
        return R.ok();
    }

    /**
     * 撤销核销单（仅 status=0 待审核可撤销）
     */
    @Operation(summary = "撤销核销单")
    @PutMapping("/fin/write-offs/{id}/cancel")
    public R<Void> cancelWriteOff(@PathVariable Long id) {
        writeOffService.cancelWriteOff(id);
        return R.ok();
    }

    /**
     * 查看核销单详情
     */
    @Operation(summary = "查看核销单详情")
    @GetMapping("/fin/write-offs/{id}")
    public R<WriteOffDetailVO> getDetail(@PathVariable Long id) {
        return R.ok(writeOffService.getDetailById(id));
    }

    // ─── 内部请求体 DTO ────────────────────────────────────────────────────────
    @lombok.Data
    public static class SubmitWriteOffRequest {
        /** 收款单ID */
        @jakarta.validation.constraints.NotNull(message = "收款单ID不能为空")
        private Long receiptId;
        /** 核销类型：1收款核销/2保证金核销/3预收款核销/4负数核销 */
        private Integer writeOffType;
        /** 核销明细列表 */
        @jakarta.validation.constraints.NotEmpty(message = "核销明细不能为空")
        private java.util.List<WriteOffDetailItemDTO> items;
    }
}
