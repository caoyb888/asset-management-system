package com.asset.report.drill;

import com.asset.common.model.R;
import com.asset.report.drill.dto.DrillDownRequestDTO;
import com.asset.report.drill.vo.DrillDownResultVO;
import com.asset.report.common.permission.RptDataScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据钻取接口
 * <p>
 * 路径前缀：/rpt/common/drill-down
 * 支持资产域四层（项目→楼栋→楼层→商铺）和财务域三层（项目→费项→应收明细）。
 * </p>
 */
@Tag(name = "数据钻取", description = "多层级数据穿透，支持资产/财务两大域")
@RestController
@RequestMapping("/rpt/common/drill-down")
@RequiredArgsConstructor
public class DrillDownController {

    private final DrillDownService drillService;

    /**
     * 执行一次钻取
     * <p>
     * 前端每次点击可下钻单元格时调用此接口，传入当前层级和维度ID，
     * 返回下一层的表格结构与数据。
     * </p>
     */
    @Operation(
            summary = "执行数据钻取",
            description = "fromLevel=1 时钻入楼栋/费项，fromLevel=2 时钻入楼层，fromLevel=3 时钻入商铺/应收明细")
    @PostMapping
    @RptDataScope
    public R<DrillDownResultVO> drillDown(@RequestBody DrillDownRequestDTO dto) {
        if (dto.getReportCode() == null || dto.getReportCode().isBlank()) {
            return R.fail("reportCode 不能为空");
        }
        if (dto.getFromLevel() != null && dto.getFromLevel() >= 4) {
            return R.fail("已到最深层级，无法继续钻取");
        }
        return R.ok(drillService.drillDown(dto));
    }

    /**
     * GET 方式钻取（兼容图表 click 事件直接绑定 URL 的场景）
     */
    @Operation(summary = "执行数据钻取（GET）", description = "适合 ECharts click 事件直接携带参数跳转")
    @GetMapping
    @RptDataScope
    public R<DrillDownResultVO> drillDownGet(
            @Parameter(description = "报表编码") @RequestParam String reportCode,
            @Parameter(description = "当前层级") @RequestParam(defaultValue = "1") Integer fromLevel,
            @Parameter(description = "维度ID") @RequestParam Long dimensionId,
            @Parameter(description = "统计日期 yyyy-MM-dd") @RequestParam(required = false) String statDate,
            @Parameter(description = "起始月份 yyyy-MM") @RequestParam(required = false) String startMonth,
            @Parameter(description = "结束月份 yyyy-MM") @RequestParam(required = false) String endMonth,
            @Parameter(description = "费项类型") @RequestParam(required = false) String feeItemType) {

        DrillDownRequestDTO dto = new DrillDownRequestDTO();
        dto.setReportCode(reportCode);
        dto.setFromLevel(fromLevel);
        dto.setDimensionId(dimensionId);
        dto.setStatDate(statDate);
        dto.setStartMonth(startMonth);
        dto.setEndMonth(endMonth);
        dto.setFeeItemType(feeItemType);
        return R.ok(drillService.drillDown(dto));
    }
}
