package com.asset.investment.contract.service;

import com.asset.common.exception.BizException;
import com.asset.investment.common.enums.ContractStatus;
import com.asset.investment.common.enums.IntentionStatus;
import com.asset.investment.contract.dto.ContractApprovalCallbackDTO;
import com.asset.investment.contract.dto.ContractSaveDTO;
import com.asset.investment.contract.entity.InvLeaseContract;
import com.asset.investment.contract.entity.InvLeaseContractVersion;
import com.asset.investment.contract.mapper.InvLeaseContractMapper;
import com.asset.investment.contract.service.impl.InvLeaseContractServiceImpl;
import com.asset.investment.engine.BillingGenerator;
import com.asset.investment.engine.RentCalculateStrategyRouter;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.service.InvIntentionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 招商合同 Service 单元测试（CTR-U-01 ~ CTR-U-10）
 * 使用 Mockito @Spy + @InjectMocks，不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("招商合同 Service 单元测试")
class InvLeaseContractServiceTest {

    @Mock
    InvLeaseContractMapper contractMapper;

    @Mock
    InvLeaseContractShopService contractShopService;

    @Mock
    InvLeaseContractFeeService contractFeeService;

    @Mock
    InvLeaseContractFeeStageService contractFeeStageService;

    @Mock
    InvLeaseContractBillingService contractBillingService;

    @Mock
    InvLeaseContractVersionService contractVersionService;

    @Mock
    InvIntentionService intentionService;

    @Mock
    BillingGenerator billingGenerator;

    @Mock
    RentCalculateStrategyRouter strategyRouter;

    @Mock
    RedissonClient redissonClient;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    JdbcTemplate jdbcTemplate;

    @Spy
    @InjectMocks
    InvLeaseContractServiceImpl service;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        // 无 Spring 容器时手动初始化 MyBatisPlus Lambda 缓存，否则 LambdaUpdateWrapper 抛异常
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                InvLeaseContract.class);
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        ReflectionTestUtils.setField(service, "baseMapper", contractMapper);

        // Redisson 锁打桩（所有 tryLock 均返回 true）
        RLock mockLock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-01 新建合同-草稿状态
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-01 新建合同-草稿状态-调用save成功")
    void createContract_setsDraftStatus() {
        ContractSaveDTO dto = new ContractSaveDTO();
        dto.setContractName("测试合同");
        dto.setProjectId(90001L);

        doReturn(0L).when(service).count(any());
        doReturn(true).when(service).save(any(InvLeaseContract.class));

        service.createContract(dto);

        verify(service, times(1)).save(any(InvLeaseContract.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-02 编辑合同-草稿可编辑
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-02 编辑合同-草稿状态-调用updateById无异常")
    void updateContract_draftStatus_callsUpdateById() {
        InvLeaseContract draft = buildContract(1L, ContractStatus.DRAFT.getCode());
        doReturn(draft).when(service).getById(1L);
        doReturn(true).when(service).updateById(any());

        service.updateContract(1L, new ContractSaveDTO());

        verify(service, times(1)).updateById(any(InvLeaseContract.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-03 编辑合同-审批中不可编辑
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-03 编辑合同-审批中状态-抛BizException")
    void updateContract_approvingStatus_throwsBizException() {
        InvLeaseContract approving = buildContract(2L, ContractStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);

        assertThatThrownBy(() -> service.updateContract(2L, new ContractSaveDTO()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不允许修改");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-04 删除合同-草稿可删
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-04 删除合同-草稿状态-调用removeById成功")
    void deleteContract_draftStatus_callsRemoveById() {
        InvLeaseContract draft = buildContract(1L, ContractStatus.DRAFT.getCode());
        doReturn(draft).when(service).getById(1L);
        doReturn(true).when(service).removeById(1L);

        service.deleteContract(1L);

        verify(service, times(1)).removeById(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-05 删除合同-审批中不可删
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-05 删除合同-审批中状态-抛BizException含'审批中'")
    void deleteContract_approvingStatus_throwsBizException() {
        InvLeaseContract approving = buildContract(2L, ContractStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);

        assertThatThrownBy(() -> service.deleteContract(2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("审批中");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-06 删除合同-生效不可删
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-06 删除合同-生效状态-抛BizException含'已生效'")
    void deleteContract_effectiveStatus_throwsBizException() {
        InvLeaseContract effective = buildContract(3L, ContractStatus.EFFECTIVE.getCode());
        doReturn(effective).when(service).getById(3L);

        assertThatThrownBy(() -> service.deleteContract(3L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已生效");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-07 发起审批-草稿→审批中
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-07 发起审批-草稿状态-调用update置status=1")
    void submitApproval_draftStatus_setsApprovingStatus() {
        InvLeaseContract draft = buildContract(1L, ContractStatus.DRAFT.getCode());
        doReturn(draft).when(service).getById(1L);
        doReturn(true).when(service).update(any());

        service.submitApproval(1L);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-08 发起审批-非草稿状态抛异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-08 发起审批-审批中状态-抛BizException含'仅草稿'")
    void submitApproval_approvingStatus_throwsBizException() {
        InvLeaseContract approving = buildContract(2L, ContractStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);

        assertThatThrownBy(() -> service.submitApproval(2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("仅草稿");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-09 审批回调-通过→生效，写版本快照
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-09 审批回调-通过-置status=2且contractVersionService.save调用1次")
    void handleApprovalCallback_approved_setsEffectiveAndSnapshot() throws Exception {
        InvLeaseContract approving = buildContract(2L, ContractStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);
        doReturn(true).when(service).update(any());
        // createSnapshot 内部调用 objectMapper.writeValueAsString 和 contractVersionService.save
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":2}");
        doReturn(true).when(contractVersionService).save(any(InvLeaseContractVersion.class));

        ContractApprovalCallbackDTO dto = new ContractApprovalCallbackDTO();
        dto.setApproved(true);

        service.handleApprovalCallback(2L, dto);

        verify(contractVersionService, times(1)).save(any(InvLeaseContractVersion.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-U-10 意向转合同-意向非通过状态抛异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-U-10 意向转合同-意向状态审批中(1)-抛BizException含'仅审批通过'")
    void convertFromIntention_intentionNotApproved_throwsBizException() {
        InvIntention approvingIntention = new InvIntention();
        approvingIntention.setId(91001L);
        approvingIntention.setStatus(IntentionStatus.APPROVING.getCode()); // status=1
        when(intentionService.getById(91001L)).thenReturn(approvingIntention);

        assertThatThrownBy(() -> service.convertFromIntention(91001L, new ContractSaveDTO()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("仅审批通过");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────────────────

    private InvLeaseContract buildContract(Long id, int status) {
        InvLeaseContract contract = new InvLeaseContract();
        contract.setId(id);
        contract.setStatus(status);
        contract.setContractCode("LC202601" + String.format("%04d", id));
        contract.setIsCurrent(1);
        return contract;
    }
}
