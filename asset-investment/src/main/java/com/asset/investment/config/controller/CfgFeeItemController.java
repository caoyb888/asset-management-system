package com.asset.investment.config.controller;

import com.asset.common.model.R;
import com.asset.investment.config.entity.CfgFeeItem;
import com.asset.investment.config.service.CfgFeeItemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收款项目配置 Controller
 */
@Tag(name = "收款项目配置", description = "招商配置-收款项目增删改查")
@RestController
@RequestMapping("/inv/config/fee-items")
@RequiredArgsConstructor
public class CfgFeeItemController {

    private final CfgFeeItemService feeItemService;

    @Operation(summary = "查询收款项目列表", description = "showAll=true 返回全部（含停用）；默认只返回启用项目，按 sort_order 排序")
    @GetMapping
    public R<List<CfgFeeItem>> list(
            @Parameter(description = "是否返回全部（含停用），管理页传 true") @RequestParam(defaultValue = "false") boolean showAll) {
        LambdaQueryWrapper<CfgFeeItem> wrapper = new LambdaQueryWrapper<CfgFeeItem>()
                .orderByAsc(CfgFeeItem::getSortOrder);
        if (!showAll) {
            wrapper.eq(CfgFeeItem::getStatus, 1);
        }
        return R.ok(feeItemService.list(wrapper));
    }

    @Operation(summary = "新增收款项目")
    @PostMapping
    public R<Long> create(@RequestBody CfgFeeItem entity) {
        // 校验必填项逻辑：租金类(itemType=1)强制 isRequired=1
        if (entity.getItemType() != null && entity.getItemType() == 1) {
            entity.setIsRequired(1);
        }
        feeItemService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑收款项目")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CfgFeeItem entity) {
        entity.setId(id);
        // 租金类强制必填
        if (entity.getItemType() != null && entity.getItemType() == 1) {
            entity.setIsRequired(1);
        }
        feeItemService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "启用/停用收款项目")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return R.fail("status 参数无效，必须为 0 或 1");
        }
        feeItemService.update(new LambdaUpdateWrapper<CfgFeeItem>()
                .eq(CfgFeeItem::getId, id)
                .set(CfgFeeItem::getStatus, status));
        return R.ok(null);
    }

    @Operation(summary = "批量更新排序", description = "传入 [{id, sortOrder}] 数组，批量写入 sort_order")
    @PutMapping("/sort")
    public R<Void> updateSort(@RequestBody List<SortItem> items) {
        items.forEach(item -> feeItemService.update(
                new LambdaUpdateWrapper<CfgFeeItem>()
                        .eq(CfgFeeItem::getId, item.getId())
                        .set(CfgFeeItem::getSortOrder, item.getSortOrder())));
        return R.ok(null);
    }

    @Operation(summary = "删除收款项目")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        feeItemService.removeById(id);
        return R.ok(null);
    }

    /** 排序更新请求体 */
    @Data
    static class SortItem {
        private Long id;
        private Integer sortOrder;
    }
}
