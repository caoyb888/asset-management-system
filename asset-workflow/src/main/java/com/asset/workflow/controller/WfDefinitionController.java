package com.asset.workflow.controller;

import com.asset.common.model.R;
import com.asset.workflow.dto.PreviewBpmnDTO;
import com.asset.workflow.dto.WfDefinitionSaveDTO;
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

    @Operation(summary = "WD-02 新增/更新流程定义（支持可视化节点配置和 XML 两种模式）")
    @PostMapping
    public R<?> save(@RequestBody WfDefinitionSaveDTO dto) {
        return R.ok(definitionService.saveDefinition(dto));
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

    @Operation(summary = "WD-04 预览 BPMN 流程图 XML（按 processKey 查库）")
    @GetMapping("/{key}/preview")
    public R<?> preview(@PathVariable String key) {
        WfProcessDefinition def = definitionService.getOne(
                new LambdaQueryWrapper<WfProcessDefinition>()
                        .eq(WfProcessDefinition::getProcessKey, key));
        if (def == null) return R.fail("流程定义不存在");
        return R.ok(def.getBpmnXml());
    }

    @Operation(summary = "WD-05 查询流程节点配置列表（可视化设计器回显）")
    @GetMapping("/{id}/nodes")
    public R<?> nodes(@PathVariable Long id) {
        return R.ok(definitionService.getNodesByDefinitionId(id));
    }

    @Operation(summary = "WD-06 根据节点配置预览生成的 BPMN XML（不保存）")
    @PostMapping("/preview-bpmn")
    public R<?> previewBpmn(@RequestBody PreviewBpmnDTO dto) {
        String xml = definitionService.previewBpmn(
                dto.getProcessKey(), dto.getProcessName(), dto.getNodeConfigs());
        return R.ok(xml);
    }
}
