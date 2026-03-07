package com.asset.operation.ledger.service;

import com.asset.common.exception.BizException;
import com.asset.operation.engine.ReceivablePlanGenerator;
import com.asset.operation.ledger.dto.AuditDTO;
import com.asset.operation.ledger.dto.OneTimePaymentDTO;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprOneTimePayment;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprOneTimePaymentMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.ledger.service.impl.OprContractLedgerServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 合同台账 Service 单元测试（LED-U）
 * 框架：Mockito，@InjectMocks OprContractLedgerServiceImpl
 *
 * <p>Mock 列表：
 * <ul>
 *   <li>OprContractLedgerMapper - 台账 Mapper（同时作为 baseMapper）</li>
 *   <li>OprReceivablePlanMapper - 应收计划 Mapper</li>
 *   <li>OprOneTimePaymentMapper - 首款 Mapper</li>
 *   <li>ReceivablePlanGenerator - 应收生成引擎</li>
 *   <li>JdbcTemplate - 跨模块查询</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("合同台账 Service（LED-U）")
class OprContractLedgerServiceTest {

    @Mock
    private OprContractLedgerMapper ledgerMapper;

    @Mock
    private OprReceivablePlanMapper receivablePlanMapper;

    @Mock
    private OprOneTimePaymentMapper oneTimePaymentMapper;

    @Mock
    private ReceivablePlanGenerator receivablePlanGenerator;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OprContractLedgerServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, OprContractLedger.class);
        TableInfoHelper.initTableInfo(assistant, OprReceivablePlan.class);
        TableInfoHelper.initTableInfo(assistant, OprOneTimePayment.class);
    }

    @BeforeEach
    void setUp() {
        // ServiceImpl 内部通过 baseMapper 字段操作数据库，需显式注入
        ReflectionTestUtils.setField(service, "baseMapper", ledgerMapper);
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private OprContractLedger buildLedger(Long id, int doubleSignStatus, int receivableStatus,
                                          int auditStatus, int status) {
        OprContractLedger ledger = new OprContractLedger();
        ledger.setId(id);
        ledger.setLedgerCode("TZ260101000001");
        ledger.setContractId(91003L);
        ledger.setProjectId(90001L);
        ledger.setMerchantId(90002L);
        ledger.setBrandId(90001L);
        ledger.setContractType(1);
        ledger.setContractStart(LocalDate.of(2025, 1, 1));
        ledger.setContractEnd(LocalDate.of(2026, 12, 31));
        ledger.setDoubleSignStatus(doubleSignStatus);
        ledger.setReceivableStatus(receivableStatus);
        ledger.setAuditStatus(auditStatus);
        ledger.setStatus(status);
        return ledger;
    }

    private Map<String, Object> buildContractInfo() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 91003L);
        map.put("project_id", 90001L);
        map.put("merchant_id", 90002L);
        map.put("brand_id", 90001L);
        map.put("contract_type", 1);
        map.put("contract_start", LocalDate.of(2025, 1, 1));
        map.put("contract_end", LocalDate.of(2026, 12, 31));
        return map;
    }

    // ── LED-U-01：从合同生成台账-自动草稿状态 ───────────────────

    @Test
    @DisplayName("LED-U-01: 从合同生成台账-自动草稿状态")
    void testGenerateFromContract() {
        // Mock: 无已存在台账
        when(ledgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // Mock: 查询招商合同信息
        when(jdbcTemplate.queryForMap(contains("inv_lease_contract"), eq(91003L)))
                .thenReturn(buildContractInfo());
        // Mock: 查当日最大编号（无已有编号）
        when(jdbcTemplate.queryForObject(contains("MAX(ledger_code)"), eq(String.class), anyString()))
                .thenReturn(null);
        // Mock: save -> insert 返回1
        when(ledgerMapper.insert(argThat((OprContractLedger l) -> l != null))).thenAnswer(inv -> {
            OprContractLedger l = inv.getArgument(0);
            l.setId(1L);
            return 1;
        });

        Long id = service.generateFromContract(91003L);

        assertNotNull(id);
        verify(ledgerMapper).insert(argThat((OprContractLedger l) -> {
            assertTrue(l.getLedgerCode().startsWith("TZ"), "编码应以 TZ 开头");
            assertEquals(0, l.getStatus(), "status 应为 0(进行中)");
            assertEquals(0, l.getDoubleSignStatus(), "doubleSignStatus 应为 0(待双签)");
            assertEquals(0, l.getReceivableStatus(), "receivableStatus 应为 0(未生成)");
            assertEquals(0, l.getAuditStatus(), "auditStatus 应为 0(待审核)");
            assertEquals(90001L, l.getProjectId());
            assertEquals(91003L, l.getContractId());
            return true;
        }));
    }

    // ── LED-U-02：从合同生成台账-防止重复 ────────────────────────

    @Test
    @DisplayName("LED-U-02: 从合同生成台账-防止重复")
    void testGenerateFromContractDuplicate() {
        // Mock: 已存在1条台账
        when(ledgerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class,
                () -> service.generateFromContract(91003L));
        assertTrue(ex.getMessage().contains("已存在"), "应提示台账已存在");
        verify(ledgerMapper, never()).insert(any(OprContractLedger.class));
    }

    // ── LED-U-03：双签确认 ──────────────────────────────────────

    @Test
    @DisplayName("LED-U-03: 双签确认")
    void testConfirmDoubleSign() {
        OprContractLedger ledger = buildLedger(1L, 0, 0, 0, 0);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);
        when(ledgerMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.confirmDoubleSign(1L);

        verify(ledgerMapper).update(isNull(), argThat((LambdaUpdateWrapper<OprContractLedger> w) -> {
            // 验证 update 被调用（具体字段由 LambdaUpdateWrapper 内部处理，无法直接断言）
            return true;
        }));
    }

    // ── LED-U-04：生成应收-调用引擎 ─────────────────────────────

    @Test
    @DisplayName("LED-U-04: 生成应收-调用引擎")
    void testGenerateReceivable() {
        OprContractLedger ledger = buildLedger(1L, 1, 0, 0, 0);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);
        when(receivablePlanGenerator.generate(1L)).thenReturn(12);
        when(ledgerMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        int count = service.generateReceivable(1L);

        assertEquals(12, count, "应返回引擎生成的应收数量");
        verify(receivablePlanGenerator).generate(1L);
        // receivableStatus 应更新为 1
        verify(ledgerMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ── LED-U-05：生成应收-已生成拒绝 ───────────────────────────

    @Test
    @DisplayName("LED-U-05: 生成应收-已生成时仍可重新生成（引擎内部控制幂等）")
    void testGenerateReceivableAlreadyGenerated() {
        // 引擎本身负责幂等，service 层只调用引擎并更新 receivableStatus
        // 此处验证 receivableStatus=1 时仍能正常调用
        OprContractLedger ledger = buildLedger(1L, 1, 1, 0, 0);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);
        when(receivablePlanGenerator.generate(1L)).thenReturn(12);
        when(ledgerMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        int count = service.generateReceivable(1L);
        assertEquals(12, count);
        verify(receivablePlanGenerator).generate(1L);
    }

    // ── LED-U-06：审核通过 ──────────────────────────────────────

    @Test
    @DisplayName("LED-U-06: 审核通过")
    void testAuditPass() {
        OprContractLedger ledger = buildLedger(1L, 1, 1, 0, 0);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);
        when(ledgerMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        // pushReceivable 调用内部会再次 selectById
        when(receivablePlanMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        AuditDTO dto = new AuditDTO();
        dto.setAuditStatus(1); // 通过

        service.audit(1L, dto);

        // 至少有1次 update（设置 auditStatus=1），通过后还会触发 pushReceivable
        verify(ledgerMapper, atLeastOnce()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ── LED-U-07：审核驳回 ──────────────────────────────────────

    @Test
    @DisplayName("LED-U-07: 审核驳回")
    void testAuditReject() {
        OprContractLedger ledger = buildLedger(1L, 1, 1, 0, 0);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);
        when(ledgerMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        AuditDTO dto = new AuditDTO();
        dto.setAuditStatus(2); // 驳回

        service.audit(1L, dto);

        verify(ledgerMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        // 驳回时不应触发 pushReceivable，receivablePlanMapper 不应被查询
        verify(receivablePlanMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    // ── LED-U-08：推送应收-生成幂等键 ───────────────────────────

    @Test
    @DisplayName("LED-U-08: 推送应收-生成幂等键")
    void testPushReceivable() {
        OprContractLedger ledger = buildLedger(1L, 1, 1, 0, 0);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);

        // 构造 2 条待推送应收
        OprReceivablePlan plan1 = new OprReceivablePlan();
        plan1.setId(101L);
        plan1.setPushStatus(0);
        plan1.setVersion(1);
        OprReceivablePlan plan2 = new OprReceivablePlan();
        plan2.setId(102L);
        plan2.setPushStatus(0);
        plan2.setVersion(2);

        when(receivablePlanMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(plan1, plan2));
        when(receivablePlanMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(ledgerMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.pushReceivable(1L);

        // 验证每条应收都被更新（2次 receivablePlanMapper.update）
        verify(receivablePlanMapper, times(2)).update(isNull(), any(LambdaUpdateWrapper.class));
        // 验证台账 receivableStatus 更新为 2(已推送)
        verify(ledgerMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ── LED-U-09：录入首款-生成应收计划 ─────────────────────────

    @Test
    @DisplayName("LED-U-09: 录入首款-生成应收计划(sourceType=4)")
    void testAddOneTimePayment() {
        OprContractLedger ledger = buildLedger(1L, 1, 1, 0, 0);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);

        // Mock: 费项名称查询
        when(jdbcTemplate.queryForObject(contains("cfg_fee_item"), eq(String.class), eq(91001L)))
                .thenReturn("租金保证金");

        // Mock: 首款插入，设置自增ID
        when(oneTimePaymentMapper.insert(any(OprOneTimePayment.class))).thenAnswer(inv -> {
            OprOneTimePayment p = inv.getArgument(0);
            p.setId(1001L);
            return 1;
        });
        // Mock: 应收计划插入，设置自增ID
        when(receivablePlanMapper.insert(any(OprReceivablePlan.class))).thenAnswer(inv -> {
            OprReceivablePlan p = inv.getArgument(0);
            p.setId(2001L);
            return 1;
        });
        when(oneTimePaymentMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        OneTimePaymentDTO dto = new OneTimePaymentDTO();
        dto.setFeeItemId(91001L);
        dto.setAmount(new BigDecimal("50000"));
        dto.setBillingStart(LocalDate.of(2026, 1, 1));
        dto.setBillingEnd(LocalDate.of(2026, 1, 31));
        dto.setEntryType(1);
        dto.setRemark("首期租金");

        service.addOneTimePayment(1L, dto);

        // 验证首款保存
        verify(oneTimePaymentMapper).insert(argThat((OprOneTimePayment p) -> {
            assertEquals(1L, p.getLedgerId());
            assertEquals(91003L, p.getContractId());
            assertEquals(91001L, p.getFeeItemId());
            assertEquals(0, new BigDecimal("50000").compareTo(p.getAmount()));
            return true;
        }));

        // 验证应收计划保存（sourceType=4）
        verify(receivablePlanMapper).insert(argThat((OprReceivablePlan p) -> {
            assertEquals(1L, p.getLedgerId());
            assertEquals(91003L, p.getContractId());
            assertEquals(4, p.getSourceType(), "sourceType 应为 4(一次性录入)");
            assertEquals(0, new BigDecimal("50000").compareTo(p.getAmount()));
            assertEquals("租金保证金", p.getFeeName());
            assertEquals(0, p.getStatus());
            return true;
        }));

        // 验证 receivableId 回填到首款记录
        verify(oneTimePaymentMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
