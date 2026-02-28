package com.asset.finance.voucher.controller;

import com.asset.common.model.R;
import com.asset.finance.voucher.dto.VoucherCreateDTO;
import com.asset.finance.voucher.dto.VoucherDetailVO;
import com.asset.finance.voucher.dto.VoucherQueryDTO;
import com.asset.finance.voucher.service.FinVoucherService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 凭证管理 Controller
 *
 * <p>凭证状态机：待审核(0) → 已审核(1) → 已上传(2)
 */
@Tag(name = "03-凭证管理")
@RestController
@RequestMapping("/fin/vouchers")
@RequiredArgsConstructor
public class FinVoucherController {

    private final FinVoucherService voucherService;

    /** 分页查询凭证列表 */
    @Operation(summary = "分页查询凭证列表")
    @GetMapping
    public R<IPage<VoucherDetailVO>> page(@Valid VoucherQueryDTO query) {
        return R.ok(voucherService.pageQuery(query));
    }

    /** 查询凭证详情（含分录） */
    @Operation(summary = "凭证详情")
    @GetMapping("/{id}")
    public R<VoucherDetailVO> getById(@PathVariable Long id) {
        return R.ok(voucherService.getDetail(id));
    }

    /** 手动创建凭证 */
    @Operation(summary = "手动创建凭证")
    @PostMapping
    public R<Long> create(@Valid @RequestBody VoucherCreateDTO dto) {
        return R.ok(voucherService.createVoucher(dto));
    }

    /** 基于收款单自动生成收款凭证 */
    @Operation(summary = "从收款单生成凭证")
    @PostMapping("/generate-from-receipt/{receiptId}")
    public R<Long> generateFromReceipt(@PathVariable Long receiptId) {
        return R.ok(voucherService.generateFromReceipt(receiptId));
    }

    /** 审核凭证（status: 0 → 1） */
    @Operation(summary = "审核凭证")
    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id) {
        voucherService.audit(id);
        return R.ok();
    }

    /** 上传凭证到财务系统（status: 1 → 2） */
    @Operation(summary = "上传凭证")
    @PostMapping("/{id}/upload")
    public R<Void> upload(@PathVariable Long id) {
        voucherService.upload(id);
        return R.ok();
    }

    /** 删除凭证（仅 status=0） */
    @Operation(summary = "删除凭证")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return R.ok();
    }
}
