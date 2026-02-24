package com.asset.investment.decomposition.controller;

import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.investment.decomposition.entity.InvRentDecompDetail;
import com.asset.investment.decomposition.entity.InvRentDecomposition;
import com.asset.investment.decomposition.service.InvRentDecompDetailService;
import com.asset.investment.decomposition.service.InvRentDecompositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租金分解 Controller
 * 按商铺类别（主力/次主力/一般）录入各格指标并汇总计算总租金
 */
@Tag(name = "06-租金分解管理", description = "租金分解CRUD与指标计算")
@RestController
@RequestMapping("/inv/rent-decomps")
@RequiredArgsConstructor
public class InvRentDecompositionController {

    private final InvRentDecompositionService decompositionService;
    private final InvRentDecompDetailService detailService;

    // ── 查询 ──────────────────────────────────────────────

    @Operation(summary = "分页查询租金分解列表")
    @GetMapping
    public R<IPage<InvRentDecomposition>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InvRentDecomposition> wrapper = new LambdaQueryWrapper<InvRentDecomposition>()
                .eq(projectId != null, InvRentDecomposition::getProjectId, projectId)
                .eq(status != null, InvRentDecomposition::getStatus, status)
                .orderByDesc(InvRentDecomposition::getCreatedAt);
        return R.ok(decompositionService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<InvRentDecomposition> detail(@PathVariable Long id) {
        return R.ok(decompositionService.getById(id));
    }

    // ── 新增 / 编辑 / 删除 ──────────────────────────────────

    @Operation(summary = "新增租金分解（草稿）")
    @PostMapping
    public R<Long> create(@RequestBody InvRentDecomposition entity) {
        entity.setStatus(0);
        entity.setDecompCode(generateCode());
        decompositionService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑租金分解基础信息")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvRentDecomposition entity) {
        entity.setId(id);
        decompositionService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除租金分解（级联删明细）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        detailService.remove(new LambdaQueryWrapper<InvRentDecompDetail>()
                .eq(InvRentDecompDetail::getDecompId, id));
        decompositionService.removeById(id);
        return R.ok(null);
    }

    // ── 明细管理 ──────────────────────────────────────────────

    @Operation(summary = "查询明细列表（按类别+ID排序）")
    @GetMapping("/{id}/details")
    public R<List<InvRentDecompDetail>> listDetails(@PathVariable Long id) {
        return R.ok(detailService.list(new LambdaQueryWrapper<InvRentDecompDetail>()
                .eq(InvRentDecompDetail::getDecompId, id)
                .orderByAsc(InvRentDecompDetail::getShopCategory)
                .orderByAsc(InvRentDecompDetail::getId)));
    }

    @Operation(summary = "批量保存明细（全量替换）")
    @PostMapping("/{id}/details")
    public R<Void> saveDetails(@PathVariable Long id,
                               @RequestBody List<InvRentDecompDetail> details) {
        detailService.remove(new LambdaQueryWrapper<InvRentDecompDetail>()
                .eq(InvRentDecompDetail::getDecompId, id));
        if (details != null && !details.isEmpty()) {
            details.forEach(d -> {
                d.setDecompId(id);
                // 自动计算年租金/年物管费
                if (d.getRentUnitPrice() != null && d.getArea() != null) {
                    d.setAnnualRent(d.getRentUnitPrice().multiply(d.getArea())
                            .multiply(BigDecimal.valueOf(12)));
                }
                if (d.getPropertyUnitPrice() != null && d.getArea() != null) {
                    d.setAnnualFee(d.getPropertyUnitPrice().multiply(d.getArea())
                            .multiply(BigDecimal.valueOf(12)));
                }
            });
            detailService.saveBatch(details);
        }
        return R.ok(null);
    }

    @Operation(summary = "自动汇总计算总租金和总物管费")
    @PostMapping("/{id}/calculate")
    public R<Map<String, Object>> calculate(@PathVariable Long id) {
        InvRentDecomposition decomp = decompositionService.getById(id);
        if (decomp == null) throw new BizException("记录不存在");

        List<InvRentDecompDetail> details = detailService.list(
                new LambdaQueryWrapper<InvRentDecompDetail>()
                        .eq(InvRentDecompDetail::getDecompId, id));

        BigDecimal totalRent = details.stream()
                .map(d -> d.getAnnualRent() != null ? d.getAnnualRent() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFee = details.stream()
                .map(d -> d.getAnnualFee() != null ? d.getAnnualFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        decomp.setTotalAnnualRent(totalRent);
        decomp.setTotalAnnualFee(totalFee);
        decompositionService.updateById(decomp);

        Map<String, Object> result = new HashMap<>();
        result.put("totalAnnualRent", totalRent);
        result.put("totalAnnualFee", totalFee);
        result.put("detailCount", details.size());
        return R.ok(result);
    }

    // ── 私有工具 ──────────────────────────────────────────────

    private String generateCode() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long count = decompositionService.count();
        return "RD" + year + String.format("%04d", count + 1);
    }
}
