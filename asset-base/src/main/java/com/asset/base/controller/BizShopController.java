package com.asset.base.controller;

import com.asset.base.model.dto.ShopQuery;
import com.asset.base.model.dto.ShopSaveDTO;
import com.asset.base.model.vo.ShopVO;
import com.asset.base.service.BizShopService;
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
 * 商铺管理 Controller
 *
 * <pre>
 * GET    /api/base/shops         分页查询
 * POST   /api/base/shops         新增商铺
 * GET    /api/base/shops/{id}    商铺详情
 * PUT    /api/base/shops/{id}    编辑商铺
 * DELETE /api/base/shops/{id}    逻辑删除
 * </pre>
 */
@Tag(name = "商铺管理", description = "基础数据-商铺增删改查")
@RestController
@RequestMapping("/base/shops")
@RequiredArgsConstructor
public class BizShopController {

    private final BizShopService shopService;

    @Operation(summary = "分页查询商铺列表")
    @GetMapping
    @OperLog(module = "商铺管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<ShopVO>> page(ShopQuery query) {
        return R.ok(shopService.pageShop(query));
    }

    @Operation(summary = "查询商铺详情")
    @GetMapping("/{id}")
    @OperLog(module = "商铺管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<ShopVO> detail(
            @Parameter(description = "商铺ID") @PathVariable Long id) {
        return R.ok(shopService.getShopById(id));
    }

    @Operation(summary = "新增商铺")
    @PostMapping
    @OperLog(module = "商铺管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody ShopSaveDTO dto) {
        return R.ok(shopService.createShop(dto));
    }

    @Operation(summary = "编辑商铺")
    @PutMapping("/{id}")
    @OperLog(module = "商铺管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "商铺ID") @PathVariable Long id,
            @Valid @RequestBody ShopSaveDTO dto) {
        shopService.updateShop(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除商铺（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "商铺管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "商铺ID") @PathVariable Long id) {
        shopService.deleteShop(id);
        return R.ok(null);
    }
}
