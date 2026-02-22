package com.asset.base.controller;

import com.asset.base.model.dto.BrandQuery;
import com.asset.base.model.dto.BrandSaveDTO;
import com.asset.base.model.vo.BrandVO;
import com.asset.base.service.BizBrandService;
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
 * 品牌管理 Controller
 *
 * <pre>
 * GET    /base/brands         分页查询
 * POST   /base/brands         新增品牌
 * GET    /base/brands/{id}    品牌详情
 * PUT    /base/brands/{id}    编辑品牌
 * DELETE /base/brands/{id}    逻辑删除
 * </pre>
 */
@Tag(name = "品牌管理", description = "基础数据-品牌增删改查")
@RestController
@RequestMapping("/base/brands")
@RequiredArgsConstructor
public class BizBrandController {

    private final BizBrandService brandService;

    @Operation(summary = "分页查询品牌列表")
    @GetMapping
    @OperLog(module = "品牌管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<BrandVO>> page(BrandQuery query) {
        return R.ok(brandService.pageBrand(query));
    }

    @Operation(summary = "查询品牌详情")
    @GetMapping("/{id}")
    @OperLog(module = "品牌管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<BrandVO> detail(
            @Parameter(description = "品牌ID") @PathVariable Long id) {
        return R.ok(brandService.getBrandById(id));
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    @OperLog(module = "品牌管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody BrandSaveDTO dto) {
        return R.ok(brandService.createBrand(dto));
    }

    @Operation(summary = "编辑品牌")
    @PutMapping("/{id}")
    @OperLog(module = "品牌管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "品牌ID") @PathVariable Long id,
            @Valid @RequestBody BrandSaveDTO dto) {
        brandService.updateBrand(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除品牌（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "品牌管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "品牌ID") @PathVariable Long id) {
        brandService.deleteBrand(id);
        return R.ok(null);
    }
}
