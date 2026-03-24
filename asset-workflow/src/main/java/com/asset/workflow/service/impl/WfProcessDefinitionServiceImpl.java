package com.asset.workflow.service.impl;

import com.asset.common.exception.BizException;
import com.asset.workflow.dto.NodeConfigDTO;
import com.asset.workflow.dto.WfDefinitionSaveDTO;
import com.asset.workflow.entity.WfNodeConfig;
import com.asset.workflow.entity.WfProcessDefinition;
import com.asset.workflow.mapper.WfNodeConfigMapper;
import com.asset.workflow.mapper.WfProcessDefinitionMapper;
import com.asset.workflow.service.BpmnGeneratorService;
import com.asset.workflow.service.WfProcessDefinitionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfProcessDefinitionServiceImpl
        extends ServiceImpl<WfProcessDefinitionMapper, WfProcessDefinition>
        implements WfProcessDefinitionService {

    private final WfNodeConfigMapper nodeConfigMapper;
    private final BpmnGeneratorService bpmnGeneratorService;

    // ── 按业务类型获取启用定义 ────────────────────────────────────────────

    @Override
    public WfProcessDefinition getByBusinessType(String businessType) {
        WfProcessDefinition def = getOne(new LambdaQueryWrapper<WfProcessDefinition>()
                .eq(WfProcessDefinition::getBusinessType, businessType)
                .eq(WfProcessDefinition::getIsEnabled, 1)
                .last("LIMIT 1"));
        if (def == null) {
            throw new BizException("未找到业务类型 [" + businessType + "] 的启用流程定义");
        }
        return def;
    }

    // ── WD-02 新增/更新流程定义 ──────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDefinition(WfDefinitionSaveDTO dto) {
        // 1. 构建 / 查找实体
        WfProcessDefinition def = (dto.getId() != null) ? getById(dto.getId()) : null;
        boolean isNew = (def == null);
        if (isNew) {
            def = new WfProcessDefinition();
        }

        // 2. 新增时检查 processKey 唯一性
        if (isNew && StringUtils.hasText(dto.getProcessKey())) {
            long cnt = count(new LambdaQueryWrapper<WfProcessDefinition>()
                    .eq(WfProcessDefinition::getProcessKey, dto.getProcessKey()));
            if (cnt > 0) {
                throw new BizException("流程 Key [" + dto.getProcessKey() + "] 已存在");
            }
        }

        // 3. 填充基础字段（processKey 不可更新）
        if (isNew && StringUtils.hasText(dto.getProcessKey())) {
            def.setProcessKey(dto.getProcessKey());
        }
        if (StringUtils.hasText(dto.getProcessName())) {
            def.setProcessName(dto.getProcessName());
        }
        if (StringUtils.hasText(dto.getBusinessType())) {
            def.setBusinessType(dto.getBusinessType());
        }
        if (dto.getIsEnabled() != null) {
            def.setIsEnabled(dto.getIsEnabled());
        } else if (isNew) {
            def.setIsEnabled(1);
        }

        // 4. 处理审批链路（可视化节点优先于 XML 源码）
        if (!CollectionUtils.isEmpty(dto.getNodeConfigs())) {
            // 可视化模式：校验 + 生成 BPMN XML
            List<WfNodeConfig> entities = toEntities(dto.getNodeConfigs());
            bpmnGeneratorService.validate(entities);
            String xml = bpmnGeneratorService.generate(
                    def.getProcessKey() != null ? def.getProcessKey() : dto.getProcessKey(),
                    def.getProcessName() != null ? def.getProcessName() : dto.getProcessName(),
                    entities);
            def.setBpmnXml(xml);
        } else if (StringUtils.hasText(dto.getBpmnXml())) {
            // XML 源码模式：直接使用
            def.setBpmnXml(dto.getBpmnXml());
        }

        // 5. 保存流程定义
        saveOrUpdate(def);

        // 6. 同步节点配置表（仅可视化模式）
        if (!CollectionUtils.isEmpty(dto.getNodeConfigs())) {
            final Long definitionId = def.getId();
            nodeConfigMapper.deleteByDefinitionId(definitionId);
            List<WfNodeConfig> nodes = toEntities(dto.getNodeConfigs());
            nodes.forEach(n -> {
                n.setDefinitionId(definitionId);
                nodeConfigMapper.insert(n);
            });
        }

        return def.getId();
    }

    // ── WD-05 查询节点配置 ────────────────────────────────────────────────

    @Override
    public List<WfNodeConfig> getNodesByDefinitionId(Long definitionId) {
        return nodeConfigMapper.selectByDefinitionId(definitionId);
    }

    // ── WD-06 预览 BPMN XML ───────────────────────────────────────────────

    @Override
    public String previewBpmn(String processKey, String processName, List<NodeConfigDTO> nodeConfigs) {
        if (CollectionUtils.isEmpty(nodeConfigs)) {
            throw new BizException("节点配置列表不能为空");
        }
        List<WfNodeConfig> entities = toEntities(nodeConfigs);
        bpmnGeneratorService.validate(entities);
        return bpmnGeneratorService.generate(processKey, processName, entities);
    }

    // ── 私有转换方法 ──────────────────────────────────────────────────────

    /**
     * 将前端 DTO 列表转换为实体列表，自动补充 START/END 节点（若缺少）
     */
    private List<WfNodeConfig> toEntities(List<NodeConfigDTO> dtos) {
        return dtos.stream().map(dto -> {
            WfNodeConfig n = new WfNodeConfig();
            n.setNodeId(dto.getNodeId());
            n.setNodeType(dto.getNodeType());
            n.setNodeName(dto.getNodeName());
            n.setNodeOrder(dto.getNodeOrder() != null ? dto.getNodeOrder() : 0);
            n.setApproverStrategy(dto.getApproverStrategy());
            n.setRoleCode(dto.getRoleCode());
            n.setUserId(dto.getUserId());
            n.setTimeoutHours(dto.getTimeoutHours());
            n.setConditionType(dto.getConditionType());
            n.setConditionOp(dto.getConditionOp());
            n.setConditionValue(dto.getConditionValue() != null
                    ? dto.getConditionValue()
                    : (dto.getConditionOp() != null ? BigDecimal.ZERO : null));
            n.setConditionExpr(dto.getConditionExpr());
            n.setRemark(dto.getRemark());
            return n;
        }).collect(Collectors.toList());
    }
}
