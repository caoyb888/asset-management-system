package com.asset.investment.contract;

import com.asset.investment.contract.entity.InvLeaseContract;
import com.asset.investment.contract.entity.InvLeaseContractShop;
import com.asset.investment.contract.entity.InvLeaseContractVersion;
import com.asset.investment.contract.mapper.InvLeaseContractMapper;
import com.asset.investment.contract.service.InvLeaseContractBillingService;
import com.asset.investment.contract.service.InvLeaseContractFeeService;
import com.asset.investment.contract.service.InvLeaseContractFeeStageService;
import com.asset.investment.contract.service.InvLeaseContractShopService;
import com.asset.investment.contract.service.InvLeaseContractVersionService;
import com.asset.investment.contract.service.impl.InvLeaseContractServiceImpl;
import com.asset.investment.engine.BillingGenerator;
import com.asset.investment.engine.RentCalculateStrategyRouter;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.service.InvIntentionService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 合同版本快照单元测试（任务9.2 - VS-01）
 * 覆盖：快照数据完整性、版本号递增、变更原因记录、快照列表排序
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("合同版本快照测试 (VS-01)")
class ContractVersionSnapshotTest {

    @Mock private InvLeaseContractMapper contractMapper;
    @Mock private InvLeaseContractShopService contractShopService;
    @Mock private InvLeaseContractFeeService contractFeeService;
    @Mock private InvLeaseContractFeeStageService contractFeeStageService;
    @Mock private InvLeaseContractBillingService contractBillingService;
    @Mock private InvLeaseContractVersionService contractVersionService;
    @Mock private InvIntentionService intentionService;
    @Mock private BillingGenerator billingGenerator;
    @Mock private RentCalculateStrategyRouter strategyRouter;
    @Mock private RedissonClient redissonClient;
    @Mock private JdbcTemplate jdbcTemplate;

    private InvLeaseContractServiceImpl contractService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeAll
    static void initMybatisPlusCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, InvLeaseContract.class);
        TableInfoHelper.initTableInfo(assistant, InvLeaseContractShop.class);
        TableInfoHelper.initTableInfo(assistant, InvLeaseContractVersion.class);
        TableInfoHelper.initTableInfo(assistant, InvIntention.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        contractService = new InvLeaseContractServiceImpl(
                contractShopService, contractFeeService, contractFeeStageService,
                contractBillingService, contractVersionService, intentionService,
                billingGenerator, strategyRouter, redissonClient,
                objectMapper, jdbcTemplate);

        var baseMapperField = CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(contractService, contractMapper);
    }

    // ─── VS-01-1：快照数据包含关键业务字段 ──────────────────────

    @Test
    @DisplayName("VS-01-1：createSnapshot 将合同关键字段序列化到 snapshotData")
    void createSnapshot_serializes_keyFields() {
        // 准备：版本为 1 的合同
        InvLeaseContract contract = buildContract(100L, 1);
        contract.setProjectId(10L);
        contract.setMerchantId(20L);
        contract.setContractStart(LocalDate.of(2026, 1, 1));
        contract.setContractEnd(LocalDate.of(2026, 12, 31));
        contract.setTotalAmount(new BigDecimal("120000.00"));

        when(contractMapper.selectById(100L)).thenReturn(contract);

        ArgumentCaptor<InvLeaseContractVersion> captor =
                ArgumentCaptor.forClass(InvLeaseContractVersion.class);
        when(contractVersionService.save(captor.capture())).thenReturn(true);

        // 执行
        contractService.createSnapshot(100L, "意向转合同");

        // 验证 save 被调用一次
        verify(contractVersionService, times(1)).save(any());

        InvLeaseContractVersion savedVersion = captor.getValue();
        // 版本号与合同一致
        assertEquals(1, savedVersion.getVersion(), "快照版本号应与合同版本号一致");
        // 关联合同ID
        assertEquals(100L, savedVersion.getContractId(), "快照应关联合同ID");
        // 变更原因记录
        assertEquals("意向转合同", savedVersion.getChangeReason(), "变更原因应被记录");
        // 快照数据不为空
        assertNotNull(savedVersion.getSnapshotData(), "snapshotData 不应为空");
        // 验证快照包含关键字段
        JsonNode snapshot = savedVersion.getSnapshotData();
        assertNotNull(snapshot.get("projectId"), "快照应包含 projectId");
        assertNotNull(snapshot.get("merchantId"), "快照应包含 merchantId");
        assertNotNull(snapshot.get("totalAmount"), "快照应包含 totalAmount");
        assertNotNull(snapshot.get("contractStart"), "快照应包含 contractStart");
        assertNotNull(snapshot.get("contractEnd"), "快照应包含 contractEnd");
    }

    @Test
    @DisplayName("VS-01-2：合同不存在时 createSnapshot 静默返回，不抛异常")
    void createSnapshot_contractNotFound_silentReturn() {
        when(contractMapper.selectById(999L)).thenReturn(null);

        // 不应抛出异常
        assertDoesNotThrow(() -> contractService.createSnapshot(999L, "测试"));
        // 不应保存任何版本记录
        verify(contractVersionService, never()).save(any());
    }

    // ─── VS-01-2：版本列表查询顺序 ────────────────────────────

    @Test
    @DisplayName("VS-01-3：listVersions 返回按版本号倒序排列的快照列表")
    void listVersions_returnsSortedByVersionDesc() {
        // 准备：3个版本快照（模拟倒序返回）
        InvLeaseContractVersion v3 = buildVersion(200L, 3, "审批通过");
        InvLeaseContractVersion v2 = buildVersion(200L, 2, "补充修改");
        InvLeaseContractVersion v1 = buildVersion(200L, 1, "意向转合同");

        when(contractVersionService.list(any(Wrapper.class))).thenReturn(List.of(v3, v2, v1));

        List<InvLeaseContractVersion> versions = contractService.listVersions(200L);

        assertEquals(3, versions.size(), "应返回3条版本记录");
        // 第一条是最新版本
        assertEquals(3, versions.get(0).getVersion(), "第一条应为最新版本(v3)");
        assertEquals(1, versions.get(2).getVersion(), "最后一条应为最早版本(v1)");
        // 变更原因保留
        assertEquals("审批通过", versions.get(0).getChangeReason(), "变更原因应被保留");
        assertEquals("意向转合同", versions.get(2).getChangeReason(), "初始原因应被保留");
    }

    @Test
    @DisplayName("VS-01-4：合同无历史版本时 listVersions 返回空列表而非 null")
    void listVersions_noHistory_returnsEmptyList() {
        when(contractVersionService.list(any(Wrapper.class))).thenReturn(List.of());

        List<InvLeaseContractVersion> versions = contractService.listVersions(300L);

        assertNotNull(versions, "返回值不应为 null");
        assertTrue(versions.isEmpty(), "无历史时应返回空列表");
    }

    // ─── VS-01-3：快照与合同版本号一致性 ─────────────────────

    @Test
    @DisplayName("VS-01-5：不同版本号的合同生成快照时，版本号字段正确记录")
    void createSnapshot_recordsCorrectVersionNumber() {
        // 版本3的合同
        InvLeaseContract contractV3 = buildContract(400L, 3);
        when(contractMapper.selectById(400L)).thenReturn(contractV3);

        ArgumentCaptor<InvLeaseContractVersion> captor =
                ArgumentCaptor.forClass(InvLeaseContractVersion.class);
        when(contractVersionService.save(captor.capture())).thenReturn(true);

        contractService.createSnapshot(400L, "合同变更后快照");

        InvLeaseContractVersion savedVersion = captor.getValue();
        assertEquals(3, savedVersion.getVersion(), "快照版本号应与合同当前版本号(3)一致");
        assertEquals("合同变更后快照", savedVersion.getChangeReason());
        assertEquals(400L, savedVersion.getContractId());
    }

    @Test
    @DisplayName("VS-01-6：snapshotData 中金额字段保留完整小数精度（不丢失分位）")
    void createSnapshot_totalAmount_precisionPreserved() {
        InvLeaseContract contract = buildContract(500L, 1);
        contract.setTotalAmount(new BigDecimal("99999.99"));
        when(contractMapper.selectById(500L)).thenReturn(contract);

        ArgumentCaptor<InvLeaseContractVersion> captor =
                ArgumentCaptor.forClass(InvLeaseContractVersion.class);
        when(contractVersionService.save(captor.capture())).thenReturn(true);

        contractService.createSnapshot(500L, "精度测试");

        JsonNode snapshot = captor.getValue().getSnapshotData();
        assertNotNull(snapshot.get("totalAmount"), "totalAmount 字段应存在");
        // BigDecimal 序列化为数字，精度不丢失
        assertEquals("99999.99", snapshot.get("totalAmount").asText(),
                "totalAmount 应精确保留至分位");
    }

    // ─── 辅助方法 ─────────────────────────────────────────────

    private InvLeaseContract buildContract(Long id, int version) {
        InvLeaseContract c = new InvLeaseContract();
        c.setId(id);
        c.setVersion(version);
        c.setContractCode("LC" + String.format("%012d", id));
        c.setContractName("测试合同-" + id);
        c.setStatus(2);
        return c;
    }

    private InvLeaseContractVersion buildVersion(Long contractId, int version, String reason) {
        InvLeaseContractVersion v = new InvLeaseContractVersion();
        v.setContractId(contractId);
        v.setVersion(version);
        v.setChangeReason(reason);
        v.setSnapshotData(objectMapper.createObjectNode().put("version", version));
        return v;
    }
}
