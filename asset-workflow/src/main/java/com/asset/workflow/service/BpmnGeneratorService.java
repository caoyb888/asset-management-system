package com.asset.workflow.service;

import com.asset.workflow.entity.WfNodeConfig;

import java.util.List;

/**
 * BPMN 生成服务
 * <p>
 * 将可视化节点配置列表转换为标准 Flowable BPMN 2.0 XML。
 * 支持节点类型：START / APPROVER / CONDITION / END
 */
public interface BpmnGeneratorService {

    /**
     * 根据节点配置列表生成 BPMN 2.0 XML 字符串
     *
     * @param processKey  流程 key（对应 BPMN process id）
     * @param processName 流程名称
     * @param nodes       节点配置列表（顺序无要求，内部按 nodeOrder 排序）
     * @return 完整的 BPMN 2.0 XML 字符串
     */
    String generate(String processKey, String processName, List<WfNodeConfig> nodes);

    /**
     * 校验节点配置合法性
     * <p>
     * 规则：
     * 1. 节点列表不能为空
     * 2. 必须包含至少 1 个 APPROVER 节点
     * 3. CONDITION 节点之后必须紧跟 APPROVER 节点
     * 4. 不允许连续出现两个 CONDITION 节点
     * 5. APPROVER 节点的 approverStrategy 不能为空
     * 6. CONDITION 节点的 conditionType 不能为空；AMOUNT 类型时 conditionValue 不能为空
     *
     * @param nodes 节点配置列表（不含 START/END，也可含，内部自动过滤）
     * @throws com.asset.common.exception.BizException 校验失败时抛出
     */
    void validate(List<WfNodeConfig> nodes);
}
