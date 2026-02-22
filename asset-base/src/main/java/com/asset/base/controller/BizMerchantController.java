package com.asset.base.controller;

import com.asset.base.model.dto.MerchantQuery;
import com.asset.base.model.dto.MerchantSaveDTO;
import com.asset.base.model.vo.MerchantVO;
import com.asset.base.service.BizMerchantService;
import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家管理 Controller
 *
 * <pre>
 * GET    /base/merchants         分页查询
 * POST   /base/merchants         新增商家
 * GET    /base/merchants/{id}    商家详情
 * PUT    /base/merchants/{id}    编辑商家
 * DELETE /base/merchants/{id}    逻辑删除
 * </pre>
 */
@Tag(name = "商家管理", description = "基础数据-商家增删改查")
@RestController
@RequestMapping("/base/merchants")
@RequiredArgsConstructor
public class BizMerchantController {

    private final BizMerchantService merchantService;

    @Operation(summary = "分页查询商家列表")
    @GetMapping
    @OperLog(module = "商家管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<MerchantVO>> page(MerchantQuery query) {
        return R.ok(merchantService.pageMerchant(query));
    }

    @Operation(summary = "查询商家详情")
    @GetMapping("/{id}")
    @OperLog(module = "商家管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<MerchantVO> detail(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        return R.ok(merchantService.getMerchantById(id));
    }

    @Operation(summary = "新增商家")
    @PostMapping
    @OperLog(module = "商家管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody MerchantSaveDTO dto) {
        return R.ok(merchantService.createMerchant(dto));
    }

    @Operation(summary = "编辑商家")
    @PutMapping("/{id}")
    @OperLog(module = "商家管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Valid @RequestBody MerchantSaveDTO dto) {
        merchantService.updateMerchant(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除商家（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "商家管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        merchantService.deleteMerchant(id);
        return R.ok(null);
    }
}
