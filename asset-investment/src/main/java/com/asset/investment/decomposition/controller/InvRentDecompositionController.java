package com.asset.investment.decomposition.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.investment.decomposition.entity.InvRentDecompDetail;
import com.asset.investment.decomposition.entity.InvRentDecomposition;
import com.asset.investment.decomposition.excel.RentDecompDetailExcel;
import com.asset.investment.decomposition.service.InvRentDecompDetailService;
import com.asset.investment.decomposition.service.InvRentDecompositionService;
import com.asset.investment.policy.entity.InvRentPolicy;
import com.asset.investment.policy.service.InvRentPolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final InvRentPolicyService policyService;

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

    @Operation(summary = "新增租金分解（草稿），自动快照关联的租决政策参数")
    @PostMapping
    public R<Long> create(@RequestBody InvRentDecomposition entity) {
        entity.setStatus(0);
        entity.setDecompCode(generateCode());
        // 快照租决政策关键参数
        if (entity.getPolicyId() != null) {
            entity.setPolicySnapshot(buildPolicySnapshot(entity.getPolicyId()));
        }
        decompositionService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "重新关联租决政策（人工触发，更新快照）")
    @PutMapping("/{id}/re-link-policy")
    public R<Void> reLinkPolicy(@PathVariable Long id,
                                @RequestBody Map<String, Object> body) {
        InvRentDecomposition decomp = decompositionService.getById(id);
        if (decomp == null) throw new BizException("记录不存在");
        Long newPolicyId = Long.valueOf(body.get("policyId").toString());
        InvRentPolicy policy = policyService.getById(newPolicyId);
        if (policy == null) throw new BizException("租决政策不存在");
        if (policy.getStatus() != 2) throw new BizException("只能关联已审批通过的政策");
        decomp.setPolicyId(newPolicyId);
        decomp.setPolicySnapshot(buildPolicySnapshot(newPolicyId));
        decompositionService.updateById(decomp);
        return R.ok(null);
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

    // ── Excel 导入 ──────────────────────────────────────────────

    @Operation(summary = "Excel批量导入租金分解明细（覆盖写入指定分解单）")
    @PostMapping("/{id}/import")
    public R<Map<String, Object>> importDetails(@PathVariable Long id,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        InvRentDecomposition decomp = decompositionService.getById(id);
        if (decomp == null) throw new BizException("租金分解记录不存在");

        List<InvRentDecompDetail> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        final int[] rowIndex = {1};

        EasyExcel.read(file.getInputStream(), RentDecompDetailExcel.class,
                new AnalysisEventListener<RentDecompDetailExcel>() {
                    @Override
                    public void invoke(RentDecompDetailExcel row, AnalysisContext ctx) {
                        rowIndex[0]++;
                        // 基础校验
                        if (row.getShopCategory() == null || row.getShopCategory() < 1 || row.getShopCategory() > 3) {
                            errors.add("第" + rowIndex[0] + "行：商铺类别须为1/2/3");
                            return;
                        }
                        if (row.getRentUnitPrice() == null || row.getRentUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                            errors.add("第" + rowIndex[0] + "行：租金单价不能为空或负数");
                            return;
                        }
                        if (row.getArea() == null || row.getArea().compareTo(BigDecimal.ZERO) <= 0) {
                            errors.add("第" + rowIndex[0] + "行：面积须大于0");
                            return;
                        }
                        InvRentDecompDetail detail = new InvRentDecompDetail();
                        detail.setDecompId(id);
                        detail.setShopCategory(row.getShopCategory());
                        detail.setFormatType(row.getFormatType());
                        detail.setRentUnitPrice(row.getRentUnitPrice());
                        detail.setPropertyUnitPrice(row.getPropertyUnitPrice() != null ? row.getPropertyUnitPrice() : BigDecimal.ZERO);
                        detail.setArea(row.getArea());
                        // 自动计算年租金/年物管费
                        detail.setAnnualRent(detail.getRentUnitPrice().multiply(detail.getArea()).multiply(BigDecimal.valueOf(12)));
                        detail.setAnnualFee(detail.getPropertyUnitPrice().multiply(detail.getArea()).multiply(BigDecimal.valueOf(12)));
                        detail.setRemark(row.getRemark());
                        validRows.add(detail);
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext ctx) {}
                }).sheet().doRead();

        if (!errors.isEmpty() && validRows.isEmpty()) {
            throw new BizException("导入失败：" + String.join("；", errors));
        }

        // 清除旧明细，批量写入新明细
        detailService.remove(new LambdaQueryWrapper<InvRentDecompDetail>()
                .eq(InvRentDecompDetail::getDecompId, id));
        if (!validRows.isEmpty()) {
            detailService.saveBatch(validRows);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", validRows.size());
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return R.ok(result);
    }

    // ── Excel 导出 ──────────────────────────────────────────────

    @Operation(summary = "导出租金分解明细Excel报表")
    @GetMapping("/{id}/export")
    public void exportDetails(@PathVariable Long id, HttpServletResponse response) throws IOException {
        InvRentDecomposition decomp = decompositionService.getById(id);
        if (decomp == null) throw new BizException("租金分解记录不存在");

        List<InvRentDecompDetail> details = detailService.list(
                new LambdaQueryWrapper<InvRentDecompDetail>()
                        .eq(InvRentDecompDetail::getDecompId, id)
                        .orderByAsc(InvRentDecompDetail::getShopCategory)
                        .orderByAsc(InvRentDecompDetail::getId));

        // 转换为 Excel 模型
        List<RentDecompDetailExcel> excelRows = new ArrayList<>();
        for (InvRentDecompDetail d : details) {
            RentDecompDetailExcel row = new RentDecompDetailExcel();
            row.setShopCategory(d.getShopCategory());
            row.setFormatType(d.getFormatType());
            row.setRentUnitPrice(d.getRentUnitPrice());
            row.setPropertyUnitPrice(d.getPropertyUnitPrice());
            row.setArea(d.getArea());
            row.setAnnualRent(d.getAnnualRent());
            row.setAnnualFee(d.getAnnualFee());
            row.setRemark(d.getRemark());
            excelRows.add(row);
        }

        String fileName = URLEncoder.encode(decomp.getDecompCode() + "_明细", StandardCharsets.UTF_8) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), RentDecompDetailExcel.class)
                .sheet("租金分解明细")
                .doWrite(excelRows);
    }

    // ── 私有工具 ──────────────────────────────────────────────

    private String generateCode() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long count = decompositionService.count();
        return "RD" + year + String.format("%04d", count + 1);
    }

    /** 构建租决政策快照（只保留关键参数，避免政策变更影响历史分解） */
    private Map<String, Object> buildPolicySnapshot(Long policyId) {
        InvRentPolicy policy = policyService.getById(policyId);
        if (policy == null) return null;
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("policyId",        policy.getId());
        snapshot.put("policyCode",      policy.getPolicyCode());
        snapshot.put("policyType",      policy.getPolicyType());
        snapshot.put("year1Rent",       policy.getYear1Rent());
        snapshot.put("year2Rent",       policy.getYear2Rent());
        snapshot.put("year1PropertyFee",policy.getYear1PropertyFee());
        snapshot.put("year2PropertyFee",policy.getYear2PropertyFee());
        snapshot.put("rentGrowthRate",  policy.getRentGrowthRate());
        snapshot.put("feeGrowthRate",   policy.getFeeGrowthRate());
        snapshot.put("depositMonths",   policy.getDepositMonths());
        snapshot.put("paymentCycle",    policy.getPaymentCycle());
        snapshot.put("snapshotAt",      java.time.LocalDateTime.now().toString());
        return snapshot;
    }
}
