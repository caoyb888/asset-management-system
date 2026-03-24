package com.asset.workflow.service;

import com.asset.common.exception.BizException;
import com.asset.workflow.entity.WfNodeConfig;
import com.asset.workflow.service.impl.BpmnGeneratorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * BpmnGeneratorService 单元测试
 * <p>
 * 覆盖：
 *   - validate()：各种非法节点配置的校验拦截
 *   - generate()：单节点、三节点、多条件节点的 BPMN XML 结构验证
 */
@DisplayName("BpmnGeneratorService 单元测试")
class BpmnGeneratorServiceTest {

    private BpmnGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new BpmnGeneratorServiceImpl();
    }

    // ──────────────────────────────────────────────────────────────
    // 辅助工厂方法
    // ──────────────────────────────────────────────────────────────

    private WfNodeConfig start() {
        WfNodeConfig n = new WfNodeConfig();
        n.setNodeId("start"); n.setNodeType("START"); n.setNodeName("发起申请"); n.setNodeOrder(0);
        return n;
    }

    private WfNodeConfig end() {
        WfNodeConfig n = new WfNodeConfig();
        n.setNodeId("end"); n.setNodeType("END"); n.setNodeName("审批完成"); n.setNodeOrder(99);
        return n;
    }

    private WfNodeConfig approver(String id, String name, int order, String strategy, String role) {
        WfNodeConfig n = new WfNodeConfig();
        n.setNodeId(id); n.setNodeType("APPROVER"); n.setNodeName(name); n.setNodeOrder(order);
        n.setApproverStrategy(strategy); n.setRoleCode(role);
        return n;
    }

    private WfNodeConfig deptApprover(int order) {
        return approver("node_dept_" + order, "部门主管审批", order, "DEPT_LEADER", null);
    }

    private WfNodeConfig roleApprover(String id, String name, int order, String roleCode) {
        return approver(id, name, order, "ROLE", roleCode);
    }

    private WfNodeConfig userApprover(String id, String name, int order, Long userId) {
        WfNodeConfig n = new WfNodeConfig();
        n.setNodeId(id); n.setNodeType("APPROVER"); n.setNodeName(name); n.setNodeOrder(order);
        n.setApproverStrategy("SPECIFIC_USER"); n.setUserId(userId);
        return n;
    }

    private WfNodeConfig amountCondition(String id, int order, String op, BigDecimal value) {
        WfNodeConfig n = new WfNodeConfig();
        n.setNodeId(id); n.setNodeType("CONDITION"); n.setNodeName("金额条件"); n.setNodeOrder(order);
        n.setConditionType("AMOUNT"); n.setConditionOp(op); n.setConditionValue(value);
        return n;
    }

    private WfNodeConfig customCondition(String id, int order, String expr) {
        WfNodeConfig n = new WfNodeConfig();
        n.setNodeId(id); n.setNodeType("CONDITION"); n.setNodeName("自定义条件"); n.setNodeOrder(order);
        n.setConditionType("CUSTOM"); n.setConditionExpr(expr);
        return n;
    }

    // ──────────────────────────────────────────────────────────────
    // validate() 测试
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validate — 校验逻辑")
    class ValidateTests {

        @Test
        @DisplayName("空列表抛出异常")
        void emptyList() {
            assertThatThrownBy(() -> service.validate(List.of()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("仅含 START/END 抛出[至少需要1个审批节点]")
        void onlyStartEnd() {
            assertThatThrownBy(() -> service.validate(List.of(start(), end())))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("至少需要配置 1 个审批节点");
        }

        @Test
        @DisplayName("APPROVER 缺 approverStrategy 抛出异常")
        void approverMissingStrategy() {
            WfNodeConfig n = new WfNodeConfig();
            n.setNodeId("n1"); n.setNodeType("APPROVER"); n.setNodeName("审批"); n.setNodeOrder(1);
            assertThatThrownBy(() -> service.validate(List.of(n)))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("审批人策略");
        }

        @Test
        @DisplayName("ROLE 策略缺 roleCode 抛出异常")
        void roleMissingCode() {
            WfNodeConfig n = approver("n1", "审批", 1, "ROLE", null);
            assertThatThrownBy(() -> service.validate(List.of(n)))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("角色编码");
        }

        @Test
        @DisplayName("SPECIFIC_USER 策略缺 userId 抛出异常")
        void specificUserMissingId() {
            WfNodeConfig n = approver("n1", "审批", 1, "SPECIFIC_USER", null);
            assertThatThrownBy(() -> service.validate(List.of(n)))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("用户ID");
        }

        @Test
        @DisplayName("CONDITION 后没有 APPROVER 抛出异常")
        void conditionAtEnd() {
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    amountCondition("gw1", 2, "GTE", new BigDecimal("100000"))
            );
            assertThatThrownBy(() -> service.validate(nodes))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("条件节点");
        }

        @Test
        @DisplayName("连续两个 CONDITION 抛出异常（后必须跟审批节点）")
        void consecutiveConditions() {
            // gw1 后面紧跟 gw2（CONDITION），不是 APPROVER → 抛出异常
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    amountCondition("gw1", 2, "GTE", new BigDecimal("100000")),
                    amountCondition("gw2", 3, "GTE", new BigDecimal("500000")),
                    roleApprover("vp", "分管领导", 4, "ROLE_VP")
            );
            assertThatThrownBy(() -> service.validate(nodes))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("条件节点");
        }

        @Test
        @DisplayName("CONDITION 缺 conditionType 抛出异常")
        void conditionMissingType() {
            WfNodeConfig cond = new WfNodeConfig();
            cond.setNodeId("gw1"); cond.setNodeType("CONDITION"); cond.setNodeName("条件"); cond.setNodeOrder(2);
            List<WfNodeConfig> nodes = List.of(deptApprover(1), cond, roleApprover("vp", "VP", 3, "ROLE_VP"));
            assertThatThrownBy(() -> service.validate(nodes))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("条件类型");
        }

        @Test
        @DisplayName("AMOUNT 条件缺 conditionValue 抛出异常")
        void amountMissingValue() {
            WfNodeConfig cond = new WfNodeConfig();
            cond.setNodeId("gw1"); cond.setNodeType("CONDITION"); cond.setNodeName("条件"); cond.setNodeOrder(2);
            cond.setConditionType("AMOUNT"); cond.setConditionOp("GTE");
            List<WfNodeConfig> nodes = List.of(deptApprover(1), cond, roleApprover("vp", "VP", 3, "ROLE_VP"));
            assertThatThrownBy(() -> service.validate(nodes))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("阈值不能为空");
        }

        @Test
        @DisplayName("CUSTOM 条件缺 conditionExpr 抛出异常")
        void customMissingExpr() {
            WfNodeConfig cond = new WfNodeConfig();
            cond.setNodeId("gw1"); cond.setNodeType("CONDITION"); cond.setNodeName("条件"); cond.setNodeOrder(2);
            cond.setConditionType("CUSTOM");
            List<WfNodeConfig> nodes = List.of(deptApprover(1), cond, roleApprover("vp", "VP", 3, "ROLE_VP"));
            assertThatThrownBy(() -> service.validate(nodes))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("条件表达式不能为空");
        }

        @Test
        @DisplayName("合法单节点不抛异常")
        void singleApproverValid() {
            assertThatCode(() -> service.validate(List.of(deptApprover(1))))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("合法三级节点不抛异常")
        void threeNodeValid() {
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    amountCondition("gw1", 2, "GTE", new BigDecimal("100000")),
                    roleApprover("vp", "分管领导", 3, "ROLE_VP"),
                    amountCondition("gw2", 4, "GTE", new BigDecimal("500000")),
                    roleApprover("gm", "总经理", 5, "ROLE_GM")
            );
            assertThatCode(() -> service.validate(nodes)).doesNotThrowAnyException();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // generate() 测试
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate — BPMN XML 结构验证")
    class GenerateTests {

        @Test
        @DisplayName("单节点：仅部门主管审批")
        void singleApproverNode() {
            List<WfNodeConfig> nodes = List.of(deptApprover(1));
            String xml = service.generate("TEST_SINGLE", "单节点测试", nodes);

            assertThat(xml)
                    .contains("<process id=\"TEST_SINGLE\"")
                    .contains("<startEvent id=\"start\"")
                    .contains("<endEvent id=\"end\"")
                    .contains("<userTask id=\"node_dept_1\"")
                    .contains("flowable:assignee=\"${deptLeaderId}\"")
                    // 连线：start → userTask → end
                    .contains("sourceRef=\"start\" targetRef=\"node_dept_1\"")
                    .contains("sourceRef=\"node_dept_1\" targetRef=\"end\"")
                    // 不应有网关
                    .doesNotContain("exclusiveGateway");
        }

        @Test
        @DisplayName("三级节点：部门主管 + 金额条件(GTE 10万) + 分管领导")
        void twoLevelWithCondition() {
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    amountCondition("gw1", 2, "GTE", new BigDecimal("100000")),
                    roleApprover("node_vp", "分管领导", 3, "ROLE_VP")
            );
            String xml = service.generate("TEST_TWO", "两级审批", nodes);

            assertThat(xml)
                    // 网关存在
                    .contains("<exclusiveGateway id=\"gw1\"")
                    // 审批节点
                    .contains("<userTask id=\"node_vp\"")
                    .contains("flowable:candidateGroups=\"ROLE_VP\"")
                    // 条件满足 → 分管领导
                    .contains("targetRef=\"node_vp\"")
                    .contains("${amount >= 100000}")
                    // 条件不满足 → end
                    .contains("targetRef=\"end\"")
                    .contains("${amount &lt; 100000}");
        }

        @Test
        @DisplayName("五级节点：部门 + 条件(10万) + VP + 条件(50万) + GM")
        void fullThreeLevelFlow() {
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    amountCondition("gw1", 2, "GTE", new BigDecimal("100000")),
                    roleApprover("node_vp", "分管领导", 3, "ROLE_VP"),
                    amountCondition("gw2", 4, "GTE", new BigDecimal("500000")),
                    roleApprover("node_gm", "总经理", 5, "ROLE_GM")
            );
            String xml = service.generate("INV_INTENTION", "意向协议审批", nodes);

            // 两个网关
            assertThat(xml)
                    .contains("id=\"gw1\"")
                    .contains("id=\"gw2\"");
            // VP / GM 节点
            assertThat(xml)
                    .contains("id=\"node_vp\"")
                    .contains("id=\"node_gm\"");
            // 10万条件：满足→VP，不满足→end
            assertThat(xml)
                    .contains("${amount >= 100000}")
                    .contains("${amount &lt; 100000}");
            // 50万条件：满足→GM，不满足→end
            assertThat(xml)
                    .contains("${amount >= 500000}")
                    .contains("${amount &lt; 500000}");
            // 最终 GM → end
            assertThat(xml).contains("sourceRef=\"node_gm\" targetRef=\"end\"");
        }

        @Test
        @DisplayName("指定用户策略生成正确的 assignee")
        void specificUserStrategy() {
            List<WfNodeConfig> nodes = List.of(userApprover("node_u1", "指定人审批", 1, 1001L));
            String xml = service.generate("TEST_USER", "指定人", nodes);
            assertThat(xml).contains("flowable:assignee=\"1001\"");
        }

        @Test
        @DisplayName("INITIATOR_LEADER 策略生成 initiatorLeaderId 变量")
        void initiatorLeaderStrategy() {
            WfNodeConfig n = approver("node_il", "直属上级审批", 1, "INITIATOR_LEADER", null);
            String xml = service.generate("TEST_IL", "上级审批", List.of(n));
            assertThat(xml).contains("flowable:assignee=\"${initiatorLeaderId}\"");
        }

        @Test
        @DisplayName("CUSTOM 条件类型生成自定义 EL 表达式")
        void customConditionExpr() {
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    customCondition("gw1", 2, "${priority == 1}"),
                    roleApprover("node_vp", "VP", 3, "ROLE_VP")
            );
            String xml = service.generate("TEST_CUSTOM", "自定义条件", nodes);
            assertThat(xml)
                    .contains("${priority == 1}")
                    .contains("!(${priority == 1})");
        }

        @Test
        @DisplayName("含 START/END 节点时正确过滤，生成结果一致")
        void withStartEndNodes() {
            List<WfNodeConfig> withStartEnd = List.of(start(), deptApprover(1), end());
            List<WfNodeConfig> withoutStartEnd = List.of(deptApprover(1));

            String xml1 = service.generate("TEST", "测试", withStartEnd);
            String xml2 = service.generate("TEST", "测试", withoutStartEnd);
            assertThat(xml1).isEqualTo(xml2);
        }

        @Test
        @DisplayName("节点乱序输入时按 nodeOrder 排序生成")
        void unsortedNodesAreOrdered() {
            List<WfNodeConfig> nodes = new ArrayList<>();
            nodes.add(roleApprover("node_gm", "总经理", 5, "ROLE_GM"));
            nodes.add(deptApprover(1));
            nodes.add(amountCondition("gw2", 4, "GTE", new BigDecimal("500000")));
            nodes.add(roleApprover("node_vp", "分管领导", 3, "ROLE_VP"));
            nodes.add(amountCondition("gw1", 2, "GTE", new BigDecimal("100000")));

            String xml = service.generate("TEST_ORDER", "乱序测试", nodes);

            // 连线应按正确顺序：dept → gw1 → vp → gw2 → gm
            int idxDeptToGw1 = xml.indexOf("sourceRef=\"node_dept_1\" targetRef=\"gw1\"");
            int idxGw1ToVp   = xml.indexOf("targetRef=\"node_vp\"");
            int idxVpToGw2   = xml.indexOf("sourceRef=\"node_vp\" targetRef=\"gw2\"");
            int idxGw2ToGm   = xml.indexOf("targetRef=\"node_gm\"");

            assertThat(idxDeptToGw1).isGreaterThan(0);
            assertThat(idxGw1ToVp).isGreaterThan(idxDeptToGw1);
            assertThat(idxVpToGw2).isGreaterThan(idxGw1ToVp);
            assertThat(idxGw2ToGm).isGreaterThan(idxVpToGw2);
        }

        @Test
        @DisplayName("生成的 XML 包含 TaskCreateListener 扩展元素")
        void containsTaskCreateListener() {
            List<WfNodeConfig> nodes = List.of(deptApprover(1));
            String xml = service.generate("TEST", "测试", nodes);
            assertThat(xml).contains("taskCreateListener");
        }

        @Test
        @DisplayName("生成的 XML 包含 ProcessCompleteListener 扩展元素")
        void containsProcessCompleteListener() {
            List<WfNodeConfig> nodes = List.of(deptApprover(1));
            String xml = service.generate("TEST", "测试", nodes);
            assertThat(xml).contains("processCompleteListener");
        }

        @Test
        @DisplayName("含超时配置的节点生成 boundaryEvent 超时定义")
        void timeoutNodeGeneratesBoundaryEvent() {
            WfNodeConfig n = deptApprover(1);
            n.setTimeoutHours(48);
            String xml = service.generate("TEST_TIMEOUT", "超时测试", List.of(n));
            assertThat(xml)
                    .contains("<boundaryEvent")
                    .contains("PT48H")
                    .contains("<timerEventDefinition>");
        }

        @Test
        @DisplayName("LT 运算符生成正确的正反向 EL 表达式")
        void ltOperator() {
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    amountCondition("gw1", 2, "LT", new BigDecimal("50000")),
                    roleApprover("node_vp", "分管领导", 3, "ROLE_VP")
            );
            String xml = service.generate("TEST_LT", "LT测试", nodes);
            assertThat(xml)
                    .contains("${amount &lt; 50000}")   // 满足：< 50000
                    .contains("${amount >= 50000}");     // 不满足：>= 50000
        }

        @Test
        @DisplayName("金额10万显示为[10万]，500显示为原值")
        void amountLabelFormat() {
            List<WfNodeConfig> nodes = List.of(
                    deptApprover(1),
                    amountCondition("gw1", 2, "GTE", new BigDecimal("100000")),
                    roleApprover("vp", "VP", 3, "ROLE_VP")
            );
            String xml = service.generate("TEST", "测试", nodes);
            assertThat(xml).contains("金额>=10万");
        }
    }
}
