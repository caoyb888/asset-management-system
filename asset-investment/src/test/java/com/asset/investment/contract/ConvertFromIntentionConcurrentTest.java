package com.asset.investment.contract;

import com.asset.common.exception.BizException;
import com.asset.investment.common.enums.IntentionStatus;
import com.asset.investment.contract.dto.ContractSaveDTO;
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
import com.asset.investment.intention.entity.InvIntentionShop;
import com.asset.investment.intention.service.InvIntentionService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 意向转合同并发锁测试（任务9 P0 - LC-02）
 * 验证 Redisson 分布式锁在多线程并发场景下只允许一次转换成功
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("意向转合同并发锁测试")
class ConvertFromIntentionConcurrentTest {

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
    @Mock private RLock mockRLock;
    @Mock private JdbcTemplate jdbcTemplate;

    private InvLeaseContractServiceImpl contractService;

    private static final Long INTENTION_ID = 1L;
    private static final Long SHOP_ID = 100L;

    /**
     * 注册 MBP Lambda 缓存，解决 LambdaQueryWrapper/LambdaUpdateWrapper 构建时的
     * "can not find lambda cache for entity" 异常（单元测试无 Spring 容器，需手动初始化）
     */
    @BeforeAll
    static void initMybatisPlusCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, InvIntention.class);
        TableInfoHelper.initTableInfo(assistant, InvLeaseContract.class);
        TableInfoHelper.initTableInfo(assistant, InvLeaseContractShop.class);
        TableInfoHelper.initTableInfo(assistant, InvLeaseContractVersion.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        // 手动构造 Service，避免 @InjectMocks 在构造器注入后不再处理父类 @Autowired baseMapper
        contractService = new InvLeaseContractServiceImpl(
                contractShopService, contractFeeService, contractFeeStageService,
                contractBillingService, contractVersionService, intentionService,
                billingGenerator, strategyRouter, redissonClient,
                new ObjectMapper(), jdbcTemplate);

        // 通过反射注入 CrudRepository.baseMapper（MBP 3.5.9 中父类 protected 字段，不在构造器参数中）
        var baseMapperField = CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(contractService, contractMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("5线程并发转合同：CAS锁模拟确保只有1个线程成功，其余因锁冲突失败")
    void concurrentConvert_onlyOneShouldSucceed() throws Exception {
        // ── 准备：已审批通过的意向协议 ──
        InvIntention intention = buildApprovedIntention();
        InvIntentionShop intentionShop = buildIntentionShop(SHOP_ID);

        when(intentionService.getById(INTENTION_ID)).thenReturn(intention);
        when(intentionService.listShops(INTENTION_ID)).thenReturn(List.of(intentionShop));
        when(intentionService.listFees(INTENTION_ID)).thenReturn(List.of());
        when(intentionService.listFeeStages(INTENTION_ID)).thenReturn(List.of());
        when(intentionService.update(any())).thenReturn(true);

        // ── 分布式锁模拟：CAS 原子操作，同一时刻只有一个线程能获取锁 ──
        AtomicBoolean lockHeld = new AtomicBoolean(false);
        when(redissonClient.getLock(anyString())).thenReturn(mockRLock);
        when(mockRLock.tryLock(anyLong(), anyLong(), any()))
                .thenAnswer(inv -> lockHeld.compareAndSet(false, true));
        doAnswer(inv -> { lockHeld.set(false); return null; }).when(mockRLock).unlock();
        when(mockRLock.isHeldByCurrentThread()).thenReturn(true);

        // ── 数据库操作（仅成功路径使用） ──
        when(contractShopService.list(any(Wrapper.class))).thenReturn(List.of());
        when(contractMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            InvLeaseContract c = inv.getArgument(0);
            c.setId(999L);
            return 1;
        }).when(contractMapper).insert(any(InvLeaseContract.class));
        when(contractMapper.selectById(999L)).thenReturn(null); // createSnapshot getById→null→提前返回
        when(contractShopService.saveBatch(any())).thenReturn(true);

        // ── 并发执行 5 个线程同时发起转合同 ──
        int threadCount = 5;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockConflictCount = new AtomicInteger(0);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        ContractSaveDTO dto = buildContractSaveDTO();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // 所有线程在此同步，同时发起请求
                    contractService.convertFromIntention(INTENTION_ID, dto);
                    successCount.incrementAndGet();
                } catch (BizException e) {
                    if (e.getMessage() != null && e.getMessage().contains("正在被其他操作占用")) {
                        lockConflictCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    // 意外异常（非预期的锁冲突，记录但不计入成功/冲突计数）
                    System.err.println("[并发测试] 意外异常: " + t.getClass().getName() + ": " + t.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // 同时释放所有线程
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "所有线程应在10秒内完成");
        assertEquals(1, successCount.get(), "只有一个线程应该成功转合同");
        assertEquals(threadCount - 1, lockConflictCount.get(), "其余线程应因分布式锁冲突而失败");
    }

    @Test
    @DisplayName("tryLock 返回 false 时：抛出包含商铺ID的 BizException")
    void tryLockFail_shouldThrowBizExceptionWithShopId() throws Exception {
        InvIntention intention = buildApprovedIntention();
        InvIntentionShop shop = buildIntentionShop(SHOP_ID);

        when(intentionService.getById(INTENTION_ID)).thenReturn(intention);
        when(intentionService.listShops(INTENTION_ID)).thenReturn(List.of(shop));
        when(redissonClient.getLock(anyString())).thenReturn(mockRLock);
        when(mockRLock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        ContractSaveDTO dto = buildContractSaveDTO();
        BizException ex = assertThrows(BizException.class,
                () -> contractService.convertFromIntention(INTENTION_ID, dto));

        assertTrue(ex.getMessage().contains(String.valueOf(SHOP_ID)),
                "异常信息应包含被锁定的商铺ID");
        assertTrue(ex.getMessage().contains("正在被其他操作占用"),
                "异常信息应说明锁冲突原因");
    }

    @Test
    @DisplayName("意向状态非审批通过时：转合同应直接拒绝，不尝试获取锁")
    void nonApprovedIntention_shouldRejectBeforeLock() {
        InvIntention intention = new InvIntention();
        intention.setId(INTENTION_ID);
        intention.setStatus(0); // 草稿状态

        when(intentionService.getById(INTENTION_ID)).thenReturn(intention);

        ContractSaveDTO dto = buildContractSaveDTO();
        BizException ex = assertThrows(BizException.class,
                () -> contractService.convertFromIntention(INTENTION_ID, dto));

        assertTrue(ex.getMessage().contains("仅审批通过的意向协议可转合同"),
                "应拒绝非审批通过状态的意向");
        // 未进入锁阶段，不应调用 getLock
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    @DisplayName("意向不存在时：抛出 BizException 提示记录不存在")
    void intentionNotFound_shouldThrowBizException() {
        when(intentionService.getById(INTENTION_ID)).thenReturn(null);

        ContractSaveDTO dto = buildContractSaveDTO();
        BizException ex = assertThrows(BizException.class,
                () -> contractService.convertFromIntention(INTENTION_ID, dto));

        assertTrue(ex.getMessage().contains("意向协议不存在"),
                "应提示意向协议不存在");
    }

    // ─── 辅助方法 ─────────────────────────────────────────────────────────────

    private InvIntention buildApprovedIntention() {
        InvIntention intention = new InvIntention();
        intention.setId(INTENTION_ID);
        intention.setStatus(IntentionStatus.APPROVED.getCode());
        intention.setProjectId(1L);
        intention.setPaymentCycle(1);
        intention.setBillingMode(1);
        return intention;
    }

    private InvIntentionShop buildIntentionShop(Long shopId) {
        InvIntentionShop shop = new InvIntentionShop();
        shop.setShopId(shopId);
        shop.setBuildingId(1L);
        shop.setFloorId(1L);
        shop.setArea(new BigDecimal("100.00"));
        return shop;
    }

    private ContractSaveDTO buildContractSaveDTO() {
        ContractSaveDTO dto = new ContractSaveDTO();
        dto.setContractName("测试合同");
        dto.setContractType(1);
        return dto;
    }
}
