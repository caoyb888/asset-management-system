package com.asset.operation.alert.controller;

import com.asset.common.model.R;
import com.asset.operation.alert.dto.AlertQueryDTO;
import com.asset.operation.alert.entity.OprAlertRecord;
import com.asset.operation.alert.service.OprAlertRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预警记录管理 Controller
 */
@Tag(name = "06-预警记录管理")
@RestController
@RequestMapping("/opr/alerts")
@RequiredArgsConstructor
public class OprAlertRecordController {

    private final OprAlertRecordService alertRecordService;

    /**
     * 分页查询预警记录
     */
    @Operation(summary = "分页查询预警记录")
    @GetMapping
    public R<IPage<OprAlertRecord>> page(AlertQueryDTO query) {
        return R.ok(alertRecordService.pageQuery(query));
    }

    /**
     * 手动取消单条预警
     */
    @Operation(summary = "手动取消预警记录")
    @Parameter(name = "id", description = "预警记录ID", required = true)
    @DeleteMapping("/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        alertRecordService.cancelById(id);
        return R.ok();
    }
}
