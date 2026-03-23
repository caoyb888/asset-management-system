package com.asset.workflow.controller;

import com.asset.common.model.R;
import com.asset.workflow.entity.WfProcessDefinition;
import com.asset.workflow.service.WfProcessDefinitionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 流程定义管理接口（管理员） — /wf/definitions
 */
@Tag(name = "04-流程定义")
@RestController
@RequestMapping("/wf/definitions")
@RequiredArgsConstructor
public class WfDefinitionController {

    private final WfProcessDefinitionService definitionService;

    @Operation(summary = "WD-01 查询流程定义列表")
    @GetMapping
    public R<?> list() {
        return R.ok(definitionService.list(new LambdaQueryWrapper<WfProcessDefinition>()
                .orderByAsc(WfProcessDefinition::getProcessKey)));
    }

    @Operation(summary = "WD-02 新增/更新流程定义")
    @PostMapping
    public R<?> save(@RequestBody WfProcessDefinition def) {
        definitionService.saveOrUpdate(def);
        return R.ok(def.getId());
    }

    @Operation(summary = "WD-03 启用/禁用流程定义")
    @PutMapping("/{id}/toggle")
    public R<?> toggle(@PathVariable Long id) {
        WfProcessDefinition def = definitionService.getById(id);
        if (def == null) return R.fail("流程定义不存在");
        definitionService.update(new LambdaUpdateWrapper<WfProcessDefinition>()
                .eq(WfProcessDefinition::getId, id)
                .set(WfProcessDefinition::getIsEnabled, def.getIsEnabled() == 1 ? 0 : 1));
        return R.ok();
    }

    @Operation(summary = "WD-04 预览 BPMN 流程图 XML")
    @GetMapping("/{key}/preview")
    public R<?> preview(@PathVariable String key) {
        WfProcessDefinition def = definitionService.getOne(
                new LambdaQueryWrapper<WfProcessDefinition>()
                        .eq(WfProcessDefinition::getProcessKey, key));
        if (def == null) return R.fail("流程定义不存在");
        return R.ok(def.getBpmnXml());
    }
}
