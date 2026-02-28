package com.asset.base.controller;

import com.alibaba.excel.EasyExcel;
import com.asset.base.excel.ShopImportListener;
import com.asset.base.mapper.BizBuildingMapper;
import com.asset.base.mapper.BizFloorMapper;
import com.asset.base.mapper.BizProjectMapper;
import com.asset.base.model.dto.ShopImportRow;
import com.asset.base.model.dto.ShopMergeDTO;
import com.asset.base.model.dto.ShopQuery;
import com.asset.base.model.dto.ShopSaveDTO;
import com.asset.base.model.dto.ShopSplitDTO;
import com.asset.base.model.vo.ShopVO;
import com.asset.base.service.BizShopService;
import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

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
    private final BizProjectMapper projectMapper;
    private final BizBuildingMapper buildingMapper;
    private final BizFloorMapper floorMapper;

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

    @Operation(summary = "拆分商铺")
    @PostMapping("/split")
    @OperLog(module = "商铺管理", action = "拆分", type = OperLog.OperType.UPDATE)
    public R<Void> split(@Valid @RequestBody ShopSplitDTO dto) {
        shopService.splitShop(dto);
        return R.ok(null);
    }

    @Operation(summary = "合并商铺")
    @PostMapping("/merge")
    @OperLog(module = "商铺管理", action = "合并", type = OperLog.OperType.UPDATE)
    public R<Void> merge(@Valid @RequestBody ShopMergeDTO dto) {
        shopService.mergeShop(dto);
        return R.ok(null);
    }

    /* ================================================================== */
    /* Excel 批量导入                                                         */
    /* ================================================================== */

    @Operation(summary = "批量导入商铺（Excel）")
    @PostMapping("/import")
    @OperLog(module = "商铺管理", action = "批量导入", type = OperLog.OperType.CREATE)
    public R<Map<String, Object>> importShops(
            @RequestParam("file") MultipartFile file) throws IOException {
        ShopImportListener listener = new ShopImportListener(
                shopService, projectMapper, buildingMapper, floorMapper);
        EasyExcel.read(file.getInputStream(), ShopImportRow.class, listener).sheet().doRead();
        return R.ok(listener.getResult());
    }

    @Operation(summary = "下载商铺导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=shop_import_template.xlsx");
        EasyExcel.write(response.getOutputStream(), ShopImportRow.class)
                .sheet("商铺导入模板")
                .doWrite(new ArrayList<>());
    }
}
