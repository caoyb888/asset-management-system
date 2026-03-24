package com.asset.workflow.service.impl;

import com.asset.common.exception.BizException;
import com.asset.workflow.entity.WfNodeConfig;
import com.asset.workflow.service.BpmnGeneratorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BPMN 生成服务实现
 * <p>
 * 节点顺序规则（nodeOrder）：
 *   0   = START（固定，可缺省，自动补充）
 *   1~98 = APPROVER / CONDITION 交替排列
 *   99  = END（固定，可缺省，自动补充）
 * <p>
 * 生成的 BPMN 连线逻辑：
 *   START → 第一个 APPROVER
 *   APPROVER → 下一个节点（CONDITION 或 END）
 *   CONDITION（网关）：
 *     满足条件 → 下一个 APPROVER
 *     不满足   → END（直接跳过后续节点，视为审批通过）
 *   最后一个 APPROVER → END
 */
@Service
public class BpmnGeneratorServiceImpl implements BpmnGeneratorService {

    private static final String NODE_TYPE_START     = "START";
    private static final String NODE_TYPE_APPROVER  = "APPROVER";
    private static final String NODE_TYPE_CONDITION = "CONDITION";
    private static final String NODE_TYPE_END       = "END";

    private static final String COND_TYPE_AMOUNT    = "AMOUNT";
    private static final String COND_TYPE_CUSTOM    = "CUSTOM";

    // 运算符映射：配置值 → EL 表达式符号 / 反向符号
    private static final java.util.Map<String, String[]> OP_MAP = new java.util.HashMap<>();
    static {
        // [正向 EL 符号, 反向 EL 符号]
        OP_MAP.put("GT",  new String[]{">",  "<="});
        OP_MAP.put("GTE", new String[]{">=", "<"});
        OP_MAP.put("LT",  new String[]{"<",  ">="});
        OP_MAP.put("LTE", new String[]{"<=", ">"});
        OP_MAP.put("EQ",  new String[]{"==", "!="});
    }

    // ──────────────────────────────────────────────────────────────────────────
    // validate
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void validate(List<WfNodeConfig> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BizException("节点列表不能为空");
        }
        List<WfNodeConfig> core = filterCore(nodes);
        if (core.isEmpty()) {
            throw new BizException("至少需要配置 1 个审批节点");
        }

        boolean prevWasCondition = false;
        for (int i = 0; i < core.size(); i++) {
            WfNodeConfig n = core.get(i);
            String type = n.getNodeType();

            if (NODE_TYPE_APPROVER.equals(type)) {
                if (n.getApproverStrategy() == null || n.getApproverStrategy().isBlank()) {
                    throw new BizException("审批节点「" + nodeName(n) + "」未配置审批人策略");
                }
                if ("ROLE".equals(n.getApproverStrategy())
                        && (n.getRoleCode() == null || n.getRoleCode().isBlank())) {
                    throw new BizException("审批节点「" + nodeName(n) + "」策略为[指定角色]时角色编码不能为空");
                }
                if ("SPECIFIC_USER".equals(n.getApproverStrategy()) && n.getUserId() == null) {
                    throw new BizException("审批节点「" + nodeName(n) + "」策略为[指定用户]时用户ID不能为空");
                }
                prevWasCondition = false;

            } else if (NODE_TYPE_CONDITION.equals(type)) {
                if (prevWasCondition) {
                    throw new BizException("不允许连续出现两个条件节点（节点：「" + nodeName(n) + "」）");
                }
                if (i == core.size() - 1) {
                    throw new BizException("条件节点「" + nodeName(n) + "」后必须跟随审批节点");
                }
                if (!NODE_TYPE_APPROVER.equals(core.get(i + 1).getNodeType())) {
                    throw new BizException("条件节点「" + nodeName(n) + "」后必须跟随审批节点");
                }
                if (n.getConditionType() == null || n.getConditionType().isBlank()) {
                    throw new BizException("条件节点「" + nodeName(n) + "」未配置条件类型");
                }
                if (COND_TYPE_AMOUNT.equals(n.getConditionType())) {
                    if (n.getConditionOp() == null || n.getConditionOp().isBlank()) {
                        throw new BizException("条件节点「" + nodeName(n) + "」未配置比较运算符");
                    }
                    if (!OP_MAP.containsKey(n.getConditionOp())) {
                        throw new BizException("条件节点「" + nodeName(n) + "」运算符值非法：" + n.getConditionOp());
                    }
                    if (n.getConditionValue() == null) {
                        throw new BizException("条件节点「" + nodeName(n) + "」金额条件阈值不能为空");
                    }
                } else if (COND_TYPE_CUSTOM.equals(n.getConditionType())) {
                    if (n.getConditionExpr() == null || n.getConditionExpr().isBlank()) {
                        throw new BizException("条件节点「" + nodeName(n) + "」自定义条件表达式不能为空");
                    }
                } else {
                    throw new BizException("条件节点「" + nodeName(n) + "」conditionType 非法：" + n.getConditionType());
                }
                prevWasCondition = true;

            } else {
                throw new BizException("节点类型非法（期望 APPROVER 或 CONDITION）：" + type);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // generate
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public String generate(String processKey, String processName, List<WfNodeConfig> nodes) {
        validate(nodes);

        List<WfNodeConfig> core = filterCore(nodes);   // 仅 APPROVER / CONDITION，按 nodeOrder 升序

        StringBuilder xml = new StringBuilder();

        // ── 文件头 ──
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n");
        xml.append("             xmlns:flowable=\"http://flowable.org/bpmn\"\n");
        xml.append("             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("             targetNamespace=\"http://asset.com/workflow\">\n\n");

        xml.append("  <process id=\"").append(escape(processKey))
           .append("\" name=\"").append(escape(processName))
           .append("\" isExecutable=\"true\">\n\n");

        // ── 节点元素 ──
        xml.append("    <!-- 开始事件 -->\n");
        xml.append("    <startEvent id=\"start\" name=\"发起申请\"/>\n\n");

        for (WfNodeConfig n : core) {
            if (NODE_TYPE_APPROVER.equals(n.getNodeType())) {
                appendUserTask(xml, n);
            } else {
                appendGateway(xml, n);
            }
        }

        xml.append("    <!-- 结束事件 -->\n");
        xml.append("    <endEvent id=\"end\" name=\"审批完成\">\n");
        xml.append("      <extensionElements>\n");
        xml.append("        <flowable:executionListener event=\"end\"\n");
        xml.append("          delegateExpression=\"${processCompleteListener}\"/>\n");
        xml.append("      </extensionElements>\n");
        xml.append("    </endEvent>\n\n");

        // ── 连线 ──
        xml.append("    <!-- 流程连线 -->\n");
        appendSequenceFlows(xml, core);

        xml.append("  </process>\n");
        xml.append("</definitions>\n");

        return xml.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 私有方法
    // ──────────────────────────────────────────────────────────────────────────

    /** 过滤并排序核心节点（去掉 START/END，仅保留 APPROVER/CONDITION） */
    private List<WfNodeConfig> filterCore(List<WfNodeConfig> nodes) {
        return nodes.stream()
                .filter(n -> NODE_TYPE_APPROVER.equals(n.getNodeType())
                          || NODE_TYPE_CONDITION.equals(n.getNodeType()))
                .sorted(Comparator.comparingInt(WfNodeConfig::getNodeOrder))
                .collect(Collectors.toList());
    }

    /** 生成 userTask 元素 */
    private void appendUserTask(StringBuilder xml, WfNodeConfig n) {
        xml.append("    <!-- ").append(escape(n.getNodeName())).append(" -->\n");
        xml.append("    <userTask id=\"").append(escape(n.getNodeId()))
           .append("\" name=\"").append(escape(n.getNodeName())).append("\"");

        String strategy = n.getApproverStrategy();
        switch (strategy) {
            case "DEPT_LEADER" ->
                xml.append("\n              flowable:assignee=\"${deptLeaderId}\"");
            case "INITIATOR_LEADER" ->
                xml.append("\n              flowable:assignee=\"${initiatorLeaderId}\"");
            case "ROLE" ->
                xml.append("\n              flowable:candidateGroups=\"").append(escape(n.getRoleCode())).append("\"");
            case "SPECIFIC_USER" ->
                xml.append("\n              flowable:assignee=\"").append(n.getUserId()).append("\"");
            default ->
                xml.append("\n              flowable:assignee=\"${deptLeaderId}\"");
        }

        if (n.getTimeoutHours() != null && n.getTimeoutHours() > 0) {
            // Flowable ISO-8601 超时：PT{n}H
            xml.append(">\n");
            xml.append("      <extensionElements>\n");
            xml.append("        <flowable:taskListener event=\"create\"\n");
            xml.append("          delegateExpression=\"${taskCreateListener}\"/>\n");
            xml.append("      </extensionElements>\n");
            xml.append("      <boundaryEvent id=\"timeout_").append(escape(n.getNodeId()))
               .append("\" attachedToRef=\"").append(escape(n.getNodeId())).append("\" cancelActivity=\"true\">\n");
            xml.append("        <timerEventDefinition>\n");
            xml.append("          <timeDuration>PT").append(n.getTimeoutHours()).append("H</timeDuration>\n");
            xml.append("        </timerEventDefinition>\n");
            xml.append("      </boundaryEvent>\n");
            xml.append("      <sequenceFlow id=\"flow_timeout_").append(escape(n.getNodeId()))
               .append("\" sourceRef=\"timeout_").append(escape(n.getNodeId()))
               .append("\" targetRef=\"end\"/>\n");
            xml.append("    </userTask>\n\n");
        } else {
            xml.append(">\n");
            xml.append("      <extensionElements>\n");
            xml.append("        <flowable:taskListener event=\"create\"\n");
            xml.append("          delegateExpression=\"${taskCreateListener}\"/>\n");
            xml.append("      </extensionElements>\n");
            xml.append("    </userTask>\n\n");
        }
    }

    /** 生成 exclusiveGateway 元素 */
    private void appendGateway(StringBuilder xml, WfNodeConfig n) {
        xml.append("    <!-- ").append(escape(n.getNodeName())).append(" -->\n");
        xml.append("    <exclusiveGateway id=\"").append(escape(n.getNodeId()))
           .append("\" name=\"").append(escape(n.getNodeName())).append("\"/>\n\n");
    }

    /**
     * 生成所有 sequenceFlow 连线
     * <p>
     * 遍历 core 节点，按顺序连接：
     *   start → core[0]
     *   core[i] → core[i+1] (APPROVER) 或分叉（CONDITION）
     *   core[last APPROVER] → end
     */
    private void appendSequenceFlows(StringBuilder xml, List<WfNodeConfig> core) {
        if (core.isEmpty()) {
            xml.append("    <sequenceFlow id=\"flow_direct\" sourceRef=\"start\" targetRef=\"end\"/>\n");
            return;
        }

        int flowSeq = 1;   // 连线编号计数器

        // start → 第一个节点
        String firstId = core.get(0).getNodeId();
        xml.append("    <sequenceFlow id=\"flow").append(flowSeq++)
           .append("\" sourceRef=\"start\" targetRef=\"").append(escape(firstId)).append("\"/>\n");

        for (int i = 0; i < core.size(); i++) {
            WfNodeConfig cur = core.get(i);

            if (NODE_TYPE_APPROVER.equals(cur.getNodeType())) {
                // APPROVER → 下一个节点（CONDITION / END）
                String nextId = (i + 1 < core.size()) ? core.get(i + 1).getNodeId() : "end";
                xml.append("    <sequenceFlow id=\"flow").append(flowSeq++)
                   .append("\" sourceRef=\"").append(escape(cur.getNodeId()))
                   .append("\" targetRef=\"").append(escape(nextId)).append("\"/>\n");

            } else {
                // CONDITION → 满足条件进入下一个 APPROVER，不满足直接到 end
                WfNodeConfig nextApprover = core.get(i + 1);  // validate 已确保后面是 APPROVER
                String[] condExprs = buildConditionExprs(cur);

                // 满足条件 → 下一个 APPROVER
                xml.append("    <sequenceFlow id=\"flow").append(flowSeq++)
                   .append("\" sourceRef=\"").append(escape(cur.getNodeId()))
                   .append("\" targetRef=\"").append(escape(nextApprover.getNodeId()))
                   .append("\" name=\"").append(escape(condExprs[2])).append("\">\n");
                xml.append("      <conditionExpression xsi:type=\"tFormalExpression\">")
                   .append(condExprs[0]).append("</conditionExpression>\n");
                xml.append("    </sequenceFlow>\n");

                // 不满足条件 → END
                xml.append("    <sequenceFlow id=\"flow").append(flowSeq++)
                   .append("\" sourceRef=\"").append(escape(cur.getNodeId()))
                   .append("\" targetRef=\"end")
                   .append("\" name=\"").append(escape(condExprs[3])).append("\">\n");
                xml.append("      <conditionExpression xsi:type=\"tFormalExpression\">")
                   .append(condExprs[1]).append("</conditionExpression>\n");
                xml.append("    </sequenceFlow>\n");
            }
        }
    }

    /**
     * 构建条件表达式
     *
     * @return [满足时EL, 不满足时EL, 满足时标签, 不满足时标签]
     */
    private String[] buildConditionExprs(WfNodeConfig n) {
        if (COND_TYPE_AMOUNT.equals(n.getConditionType())) {
            String[] ops = OP_MAP.get(n.getConditionOp());   // [正向, 反向]
            BigDecimal val = n.getConditionValue();
            String valStr = formatAmount(val);
            String labelYes = "金额" + ops[0] + valStr;
            String labelNo  = "金额" + ops[1] + valStr;
            // EL 中 > / >= 不需要 XML 转义，< / <= 需要
            String elYes = "${amount " + elOp(ops[0]) + " " + val.toPlainString() + "}";
            String elNo  = "${amount " + elOp(ops[1]) + " " + val.toPlainString() + "}";
            return new String[]{elYes, elNo, labelYes, labelNo};
        } else {
            // CUSTOM：conditionExpr 已是完整 EL，反向取非
            String expr = n.getConditionExpr().trim();
            return new String[]{expr, "!(" + expr + ")", "满足条件", "不满足条件"};
        }
    }

    /** 将 > < >= <= 转换为 XML 中可用的形式（< 需转义为 &lt;，其他不变） */
    private String elOp(String op) {
        return switch (op) {
            case "<"  -> "&lt;";
            case "<=" -> "&lt;=";
            default   -> op;   // > >= == != 不需要转义
        };
    }

    /** 金额格式化（≥1万显示万，否则显示原值） */
    private String formatAmount(BigDecimal val) {
        if (val == null) return "0";
        if (val.compareTo(new BigDecimal("10000")) >= 0) {
            BigDecimal wan = val.divide(new BigDecimal("10000"), 0, java.math.RoundingMode.UNNECESSARY);
            return wan.toPlainString() + "万";
        }
        return val.toPlainString();
    }

    /** XML 特殊字符转义（仅处理属性值，&lt; 在 EL 中已单独处理） */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
        // > 在 XML 属性值中无需转义，保持可读性
        // < 在属性值中不需转义，在文本内容中由 elOp() 处理
    }

    private String nodeName(WfNodeConfig n) {
        return n.getNodeName() != null ? n.getNodeName() : n.getNodeId();
    }
}
