package com.asset.report.schedule;

import com.asset.common.model.R;
import com.asset.report.schedule.dto.ScheduleTaskDTO;
import com.asset.report.schedule.vo.ScheduleTaskVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 报表定时推送任务 CRUD 接口
 * <p>
 * 路径前缀：/rpt/common/schedule-tasks
 * </p>
 */
@Tag(name = "报表定时推送", description = "定时推送任务管理（CRUD + 启用/禁用）")
@RestController
@RequestMapping("/rpt/common/schedule-tasks")
@RequiredArgsConstructor
public class ScheduleTaskController {

    private final ScheduleTaskService taskService;

    /**
     * 分页查询任务列表
     */
    @Operation(summary = "分页查询", description = "支持按任务名称/报表编码关键字搜索")
    @GetMapping
    public R<IPage<ScheduleTaskVO>> page(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(taskService.page(keyword, pageNum, pageSize));
    }

    /**
     * 查询单条任务详情
     */
    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<ScheduleTaskVO> detail(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return R.ok(taskService.getById(id));
    }

    /**
     * 创建定时推送任务
     */
    @Operation(
        summary = "创建任务",
        description = "Cron 表达式使用 Spring 6位格式（秒 分 时 日 月 周），示例：每天8:30 = `0 30 8 * * ?`"
    )
    @PostMapping
    public R<Long> create(@Valid @RequestBody ScheduleTaskDTO dto) {
        return R.ok(taskService.create(dto));
    }

    /**
     * 更新定时推送任务
     */
    @Operation(summary = "更新任务")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Valid @RequestBody ScheduleTaskDTO dto) {
        taskService.update(id, dto);
        return R.ok();
    }

    /**
     * 删除任务（逻辑删除）
     */
    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    /**
     * 切换启用/禁用
     */
    @Operation(summary = "启用/禁用", description = "切换任务状态；启用时自动重新计算 nextRunTime；同时重置连续失败计数")
    @PutMapping("/{id}/toggle")
    public R<Integer> toggle(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        int newStatus = taskService.toggle(id);
        return R.ok(newStatus);
    }
}
