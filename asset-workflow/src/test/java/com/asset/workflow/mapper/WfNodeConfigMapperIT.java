package com.asset.workflow.mapper;

import com.asset.workflow.WorkflowApplication;
import com.asset.workflow.entity.WfNodeConfig;
import com.asset.workflow.feign.SystemFeignClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WfNodeConfigMapper 集成测试
 * <p>
 * 使用真实 MySQL（asset_db），每个测试在事务内执行并自动回滚。
 * SystemFeignClient 使用 MockBean，避免依赖 asset-system 服务。
 */
@SpringBootTest(
        classes = WorkflowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false"
        }
)
@ActiveProfiles("test")
@Transactional
@DisplayName("WfNodeConfigMapper 集成测试")
class WfNodeConfigMapperIT {

    @Autowired
    private WfNodeConfigMapper mapper;

    // Feign 客户端不参与 Mapper 测试，Mock 掉以避免 Feign 初始化依赖
    @MockBean
    private SystemFeignClient systemFeignClient;

    // ── 固定测试用 definitionId（不干扰生产数据，事务回滚后自动清理） ──
    private static final Long DEF_ID = 9990001L;

    // ─────────────────────────────────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────────────────────────────────

    private WfNodeConfig buildNode(String nodeId, String nodeType, String nodeName, int order) {
        WfNodeConfig n = new WfNodeConfig();
        n.setDefinitionId(DEF_ID);
        n.setNodeId(nodeId);
        n.setNodeType(nodeType);
        n.setNodeName(nodeName);
        n.setNodeOrder(order);
        return n;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // insert（BaseMapper）
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("insert - 单条插入")
    class InsertTests {

        @Test
        @DisplayName("插入 START 节点后 id 自增")
        void insertStartNode_idAssigned() {
            WfNodeConfig node = buildNode("start", "START", "发起申请", 0);
            int rows = mapper.insert(node);
            assertThat(rows).isEqualTo(1);
            assertThat(node.getId()).isNotNull().isPositive();
        }

        @Test
        @DisplayName("插入 APPROVER 节点包含审批策略字段")
        void insertApproverNode_strategyPersisted() {
            WfNodeConfig node = buildNode("node_dept", "APPROVER", "部门主管审批", 1);
            node.setApproverStrategy("DEPT_LEADER");
            mapper.insert(node);

            WfNodeConfig loaded = mapper.selectById(node.getId());
            assertThat(loaded).isNotNull();
            assertThat(loaded.getApproverStrategy()).isEqualTo("DEPT_LEADER");
            assertThat(loaded.getNodeType()).isEqualTo("APPROVER");
        }

        @Test
        @DisplayName("插入 CONDITION 节点包含条件字段")
        void insertConditionNode_conditionPersisted() {
            WfNodeConfig node = buildNode("gw_amount", "CONDITION", "金额条件", 2);
            node.setConditionType("AMOUNT");
            node.setConditionOp("GTE");
            node.setConditionValue(new BigDecimal("100000.00"));
            mapper.insert(node);

            WfNodeConfig loaded = mapper.selectById(node.getId());
            assertThat(loaded.getConditionType()).isEqualTo("AMOUNT");
            assertThat(loaded.getConditionOp()).isEqualTo("GTE");
            assertThat(loaded.getConditionValue()).isEqualByComparingTo(new BigDecimal("100000.00"));
        }

        @Test
        @DisplayName("插入 ROLE 策略节点包含 roleCode")
        void insertRoleApproverNode_roleCodePersisted() {
            WfNodeConfig node = buildNode("node_vp", "APPROVER", "分管领导审批", 3);
            node.setApproverStrategy("ROLE");
            node.setRoleCode("ROLE_VP");
            mapper.insert(node);

            WfNodeConfig loaded = mapper.selectById(node.getId());
            assertThat(loaded.getApproverStrategy()).isEqualTo("ROLE");
            assertThat(loaded.getRoleCode()).isEqualTo("ROLE_VP");
        }

        @Test
        @DisplayName("插入 SPECIFIC_USER 策略节点包含 userId")
        void insertSpecificUserNode_userIdPersisted() {
            WfNodeConfig node = buildNode("node_gm", "APPROVER", "总经理审批", 5);
            node.setApproverStrategy("SPECIFIC_USER");
            node.setUserId(1001L);
            mapper.insert(node);

            WfNodeConfig loaded = mapper.selectById(node.getId());
            assertThat(loaded.getApproverStrategy()).isEqualTo("SPECIFIC_USER");
            assertThat(loaded.getUserId()).isEqualTo(1001L);
        }

        @Test
        @DisplayName("插入超时节点包含 timeoutHours")
        void insertNodeWithTimeout_timeoutPersisted() {
            WfNodeConfig node = buildNode("node_timeout", "APPROVER", "限时审批", 2);
            node.setApproverStrategy("DEPT_LEADER");
            node.setTimeoutHours(48);
            mapper.insert(node);

            WfNodeConfig loaded = mapper.selectById(node.getId());
            assertThat(loaded.getTimeoutHours()).isEqualTo(48);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // selectByDefinitionId
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("selectByDefinitionId - 按定义ID查询")
    class SelectByDefinitionIdTests {

        @Test
        @DisplayName("无节点时返回空列表")
        void noNodes_returnsEmpty() {
            List<WfNodeConfig> result = mapper.selectByDefinitionId(DEF_ID);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("插入 3 个节点后按 definitionId 查到全部")
        void threeNodes_allReturned() {
            mapper.insert(buildNode("start", "START", "发起申请", 0));
            mapper.insert(buildNode("node_1", "APPROVER", "部门主管审批", 1));
            mapper.insert(buildNode("end", "END", "审批完成", 99));

            List<WfNodeConfig> result = mapper.selectByDefinitionId(DEF_ID);
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("结果按 node_order 升序排列")
        void nodesReturnedInOrder() {
            // 故意乱序插入
            mapper.insert(buildNode("end", "END", "审批完成", 99));
            mapper.insert(buildNode("node_1", "APPROVER", "审批", 1));
            mapper.insert(buildNode("start", "START", "发起", 0));

            List<WfNodeConfig> result = mapper.selectByDefinitionId(DEF_ID);
            assertThat(result).extracting(WfNodeConfig::getNodeOrder)
                    .containsExactly(0, 1, 99);
        }

        @Test
        @DisplayName("不同 definitionId 的节点互不干扰")
        void differentDefinitions_isolated() {
            Long otherId = 9990002L;
            mapper.insert(buildNode("start", "START", "发起申请", 0)); // DEF_ID
            WfNodeConfig other = buildNode("start", "START", "发起申请", 0);
            other.setDefinitionId(otherId);
            mapper.insert(other);

            assertThat(mapper.selectByDefinitionId(DEF_ID)).hasSize(1);
            assertThat(mapper.selectByDefinitionId(otherId)).hasSize(1);
        }

        @Test
        @DisplayName("逻辑删除的节点不被查出")
        void deletedNodes_notReturned() {
            WfNodeConfig node = buildNode("node_del", "APPROVER", "待删节点", 1);
            mapper.insert(node);
            // 通过 BaseMapper.deleteById 触发 @TableLogic 逻辑删除（设 is_deleted=1）
            mapper.deleteById(node.getId());

            List<WfNodeConfig> result = mapper.selectByDefinitionId(DEF_ID);
            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteByDefinitionId
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteByDefinitionId - 按定义ID逻辑删除")
    class DeleteByDefinitionIdTests {

        @Test
        @DisplayName("删除后 selectByDefinitionId 返回空")
        void deleteAll_queryReturnsEmpty() {
            mapper.insert(buildNode("start", "START", "发起申请", 0));
            mapper.insert(buildNode("node_1", "APPROVER", "部门主管审批", 1));
            mapper.insert(buildNode("end", "END", "审批完成", 99));

            mapper.deleteByDefinitionId(DEF_ID);

            assertThat(mapper.selectByDefinitionId(DEF_ID)).isEmpty();
        }

        @Test
        @DisplayName("删除仅影响指定 definitionId，其他定义节点不受影响")
        void deleteOnlyAffectsTargetDefinition() {
            Long otherId = 9990003L;
            mapper.insert(buildNode("node_a", "APPROVER", "节点A", 1)); // DEF_ID

            WfNodeConfig other = buildNode("node_b", "APPROVER", "节点B", 1);
            other.setDefinitionId(otherId);
            mapper.insert(other);

            mapper.deleteByDefinitionId(DEF_ID);

            assertThat(mapper.selectByDefinitionId(DEF_ID)).isEmpty();
            assertThat(mapper.selectByDefinitionId(otherId)).hasSize(1);
        }

        @Test
        @DisplayName("删除空定义不报错")
        void deleteEmpty_noException() {
            // 无节点，直接调用不应抛异常
            mapper.deleteByDefinitionId(99999999L);
        }

        @Test
        @DisplayName("先删后插可重新查询到新节点")
        void deleteAndReinsert_queryFindsNew() {
            mapper.insert(buildNode("old_node", "APPROVER", "旧节点", 1));

            mapper.deleteByDefinitionId(DEF_ID);

            WfNodeConfig newNode = buildNode("new_node", "APPROVER", "新节点", 1);
            mapper.insert(newNode);

            List<WfNodeConfig> result = mapper.selectByDefinitionId(DEF_ID);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNodeId()).isEqualTo("new_node");
        }
    }
}
