package com.asset.finance.deposit;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.deposit.dto.*;
import com.asset.finance.deposit.entity.FinDepositAccount;
import com.asset.finance.deposit.entity.FinDepositTransaction;
import com.asset.finance.deposit.mapper.FinDepositAccountMapper;
import com.asset.finance.deposit.mapper.FinDepositTransactionMapper;
import com.asset.finance.deposit.service.FinDepositService;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * 保证金管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>DEP-01：余额恒等式：balance = total_in - total_offset - total_refund - total_forfeit</li>
 *   <li>DEP-02：余额不足时冲抵 → 抛出 FIN_4002</li>
 *   <li>DEP-03：退款审批通过后余额恒等式</li>
 *   <li>DEP-04：罚没审批通过后余额恒等式</li>
 *   <li>DEP-05：审批回调重复触发幂等</li>
 *   <li>DEP-06：查询账户信息-含统计字段</li>
 *   <li>DEP-07：流水分页查询-按交易类型筛选</li>
 *   <li>DEP-08：冲抵审批通过-同时更新应收</li>
 *   <li>DEP-09：审批驳回-余额不变</li>
 *   <li>DEP-10：连续缴纳-余额累加</li>
 * </ol>
 */
@DisplayName("保证金管理 Service 测试")
class FinDepositServiceTest extends FinanceTestBase {

    @Autowired
    private FinDepositService depositService;

    @Autowired
    private FinDepositAccountMapper accountMapper;

    @Autowired
    private FinDepositTransactionMapper transactionMapper;

    @Autowired
    private FinReceivableMapper receivableMapper;

    private static final Long CONTRACT_ID = 10003L;
    private static final Long MERCHANT_ID = 20003L;

    @BeforeEach
    void setUp() {
        // 预先缴纳5000元保证金，建立账户
        DepositPayInDTO payIn = new DepositPayInDTO();
        payIn.setContractId(CONTRACT_ID);
        payIn.setAmount(new BigDecimal("5000.00"));
        payIn.setSourceCode("RC-TEST-PAY");
        payIn.setReason("初始缴纳");
        depositService.payIn(payIn);
    }

    // ─── 场景1：余额恒等式 ────────────────────────────────────────────────────

    @Test
    @DisplayName("场景1：缴纳后余额恒等式 balance = total_in - offset - refund - forfeit")
    void balanceIdentity_afterPayIn() {
        FinDepositAccount account = accountMapper.selectByContractId(CONTRACT_ID);
        assertThat(account).isNotNull();

        BigDecimal computed = account.getTotalIn()
                .subtract(account.getTotalOffset() == null ? BigDecimal.ZERO : account.getTotalOffset())
                .subtract(account.getTotalRefund() == null ? BigDecimal.ZERO : account.getTotalRefund())
                .subtract(account.getTotalForfeit() == null ? BigDecimal.ZERO : account.getTotalForfeit());

        assertThat(account.getBalance())
                .as("balance 必须满足恒等式 = total_in - offset - refund - forfeit")
                .isEqualByComparingTo(computed);
    }

    // ─── 场景2：余额不足时冲抵 → FIN_4002 ────────────────────────────────────

    @Test
    @DisplayName("场景2：余额不足冲抵 → 抛出 FIN_4002")
    void offset_insufficientBalance_shouldThrowFIN4002() {
        // 插入一条有效应收记录
        FinReceivable ar = buildReceivable(CONTRACT_ID, new BigDecimal("10000.00"));
        receivableMapper.insert(ar);

        DepositOffsetDTO dto = new DepositOffsetDTO();
        dto.setContractId(CONTRACT_ID);
        dto.setReceivableId(ar.getId());
        dto.setAmount(new BigDecimal("9999.00")); // 超出余额5000

        assertThatThrownBy(() -> depositService.processOffset(dto))
                .isInstanceOf(FinBizException.class)
                .extracting(e -> ((FinBizException) e).getCode())
                .isEqualTo(FinErrorCode.FIN_4002.getCode());
    }

    // ─── 场景3：审批通过后余额恒等式仍成立 ───────────────────────────────────

    @Test
    @DisplayName("场景3：退款审批通过后余额恒等式保持成立")
    void refundApproved_balanceIdentityMaintained() {
        // 发起退款申请（1000元）
        DepositRefundDTO refundDTO = new DepositRefundDTO();
        refundDTO.setContractId(CONTRACT_ID);
        refundDTO.setAmount(new BigDecimal("1000.00"));
        refundDTO.setReason("合同到期退款");
        Long txId = depositService.processRefund(refundDTO);

        // 找到流水，设置 approvalId
        FinDepositTransaction tx = transactionMapper.selectById(txId);
        String approvalId = "MOCK-DEPOSIT-APPR-" + txId;
        tx.setApprovalId(approvalId);
        transactionMapper.updateById(tx);

        // 模拟审批通过
        depositService.approveCallback(approvalId, true);

        // 验证余额恒等式
        FinDepositAccount account = accountMapper.selectByContractId(CONTRACT_ID);
        BigDecimal computed = account.getTotalIn()
                .subtract(safeValue(account.getTotalOffset()))
                .subtract(safeValue(account.getTotalRefund()))
                .subtract(safeValue(account.getTotalForfeit()));

        assertThat(account.getBalance())
                .as("退款后余额恒等式应成立")
                .isEqualByComparingTo(computed);

        assertThat(account.getBalance())
                .as("余额应从5000变为4000")
                .isEqualByComparingTo("4000.00");

        assertThat(account.getTotalRefund())
                .as("累计退款应为1000")
                .isEqualByComparingTo("1000.00");
    }

    // ─── 场景4：罚没后余额恒等式 ──────────────────────────────────────────────

    @Test
    @DisplayName("场景4：罚没审批通过后余额恒等式保持成立")
    void forfeitApproved_balanceIdentityMaintained() {
        DepositForfeitDTO forfeitDTO = new DepositForfeitDTO();
        forfeitDTO.setContractId(CONTRACT_ID);
        forfeitDTO.setAmount(new BigDecimal("500.00"));
        forfeitDTO.setReason("违规罚没");
        Long txId = depositService.processForfeit(forfeitDTO);

        FinDepositTransaction tx = transactionMapper.selectById(txId);
        String approvalId = "MOCK-FORFEIT-APPR-" + txId;
        tx.setApprovalId(approvalId);
        transactionMapper.updateById(tx);

        depositService.approveCallback(approvalId, true);

        FinDepositAccount account = accountMapper.selectByContractId(CONTRACT_ID);
        BigDecimal computed = account.getTotalIn()
                .subtract(safeValue(account.getTotalOffset()))
                .subtract(safeValue(account.getTotalRefund()))
                .subtract(safeValue(account.getTotalForfeit()));

        assertThat(account.getBalance())
                .as("罚没后余额恒等式应成立")
                .isEqualByComparingTo(computed);

        assertThat(account.getBalance())
                .as("余额应从5000变为4500")
                .isEqualByComparingTo("4500.00");
    }

    // ─── 场景5：重复回调幂等性 ─────────────────────────────────────────────────

    @Test
    @DisplayName("场景5：审批回调重复触发幂等：余额不重复扣减")
    void approveCallback_idempotent_balanceDeductedOnce() {
        DepositRefundDTO refundDTO = new DepositRefundDTO();
        refundDTO.setContractId(CONTRACT_ID);
        refundDTO.setAmount(new BigDecimal("300.00"));
        refundDTO.setReason("测试幂等");
        Long txId = depositService.processRefund(refundDTO);

        FinDepositTransaction tx = transactionMapper.selectById(txId);
        String approvalId = "MOCK-IDEM-APPR-" + txId;
        tx.setApprovalId(approvalId);
        transactionMapper.updateById(tx);

        // 第一次回调
        depositService.approveCallback(approvalId, true);
        FinDepositAccount after1 = accountMapper.selectByContractId(CONTRACT_ID);
        BigDecimal balanceAfter1 = after1.getBalance();

        // 第二次重复回调（幂等保护，应被忽略）
        depositService.approveCallback(approvalId, true);
        FinDepositAccount after2 = accountMapper.selectByContractId(CONTRACT_ID);

        assertThat(after2.getBalance())
                .as("重复回调余额不应再次扣减")
                .isEqualByComparingTo(balanceAfter1);
    }

    // ─── DEP-06：查询账户信息-含统计字段 ────────────────────────────────────────

    @Test
    @DisplayName("DEP-06：getAccount 返回含 balance/totalIn 等统计字段")
    void getAccount_shouldContainStatisticsFields() {
        DepositAccountVO vo = depositService.getAccount(CONTRACT_ID);

        assertThat(vo).as("账户VO不应为空").isNotNull();
        assertThat(vo.getBalance())
                .as("余额应为5000")
                .isEqualByComparingTo("5000.00");
        assertThat(vo.getTotalIn())
                .as("累计缴纳应为5000")
                .isEqualByComparingTo("5000.00");
        assertThat(vo.getContractId())
                .as("合同ID应匹配")
                .isEqualTo(CONTRACT_ID);
    }

    // ─── DEP-07：流水分页查询-按交易类型筛选 ──────────────────────────────────────

    @Test
    @DisplayName("DEP-07：pageTransaction 按 transType=1 仅返回收入流水")
    void pageTransaction_byTransType_shouldFilter() {
        // 发起一笔退款（transType=3）
        DepositRefundDTO refundDTO = new DepositRefundDTO();
        refundDTO.setContractId(CONTRACT_ID);
        refundDTO.setAmount(new BigDecimal("100.00"));
        refundDTO.setReason("测试退款");
        depositService.processRefund(refundDTO);

        // 按 transType=1（收入）查询
        DepositQueryDTO query = new DepositQueryDTO();
        query.setContractId(CONTRACT_ID);
        query.setTransType(1);
        query.setPageNum(1);
        query.setPageSize(50);

        IPage<FinDepositTransaction> page = depositService.pageTransaction(query);

        assertThat(page.getRecords())
                .as("按transType=1筛选应仅返回收入流水")
                .isNotEmpty()
                .allSatisfy(tx -> assertThat(tx.getTransType()).isEqualTo(1));
    }

    // ─── DEP-08：冲抵审批通过-同时更新应收 ────────────────────────────────────────

    @Test
    @DisplayName("DEP-08：冲抵审批通过后 account.balance=4000，应收 received+=1000")
    void offsetApproved_shouldUpdateReceivable() {
        // 插入应收1000
        FinReceivable ar = buildReceivable(CONTRACT_ID, new BigDecimal("1000.00"));
        receivableMapper.insert(ar);

        // 发起冲抵
        DepositOffsetDTO dto = new DepositOffsetDTO();
        dto.setContractId(CONTRACT_ID);
        dto.setReceivableId(ar.getId());
        dto.setAmount(new BigDecimal("1000.00"));

        Long txId = depositService.processOffset(dto);

        // 模拟审批通过
        FinDepositTransaction tx = transactionMapper.selectById(txId);
        String approvalId = "MOCK-OFFSET-APPR-" + txId;
        tx.setApprovalId(approvalId);
        transactionMapper.updateById(tx);
        depositService.approveCallback(approvalId, true);

        // 验证账户
        FinDepositAccount account = accountMapper.selectByContractId(CONTRACT_ID);
        assertThat(account.getBalance())
                .as("冲抵后余额应为4000")
                .isEqualByComparingTo("4000.00");
        assertThat(account.getTotalOffset())
                .as("累计冲抵应为1000")
                .isEqualByComparingTo("1000.00");

        // 验证应收
        FinReceivable updatedAr = receivableMapper.selectById(ar.getId());
        assertThat(updatedAr.getReceivedAmount())
                .as("应收已收金额应增加1000")
                .isEqualByComparingTo("1000.00");
        assertThat(updatedAr.getOutstandingAmount())
                .as("应收欠费应为0")
                .isEqualByComparingTo("0.00");
    }

    // ─── DEP-09：审批驳回-余额不变 ────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-09：退款审批驳回后余额不变，流水status=2")
    void refundRejected_shouldNotChangeBalance() {
        DepositRefundDTO refundDTO = new DepositRefundDTO();
        refundDTO.setContractId(CONTRACT_ID);
        refundDTO.setAmount(new BigDecimal("500.00"));
        refundDTO.setReason("测试驳回");
        Long txId = depositService.processRefund(refundDTO);

        FinDepositTransaction tx = transactionMapper.selectById(txId);
        String approvalId = "MOCK-REJECT-APPR-" + txId;
        tx.setApprovalId(approvalId);
        transactionMapper.updateById(tx);

        depositService.approveCallback(approvalId, false);

        // 验证余额不变
        FinDepositAccount account = accountMapper.selectByContractId(CONTRACT_ID);
        assertThat(account.getBalance())
                .as("驳回后余额应仍为5000")
                .isEqualByComparingTo("5000.00");

        // 验证流水状态
        FinDepositTransaction updatedTx = transactionMapper.selectById(txId);
        assertThat(updatedTx.getStatus())
                .as("驳回后流水状态应为2")
                .isEqualTo(2);
    }

    // ─── DEP-10：连续缴纳-余额累加 ────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-10：连续缴纳3000+2000后余额为10000")
    void consecutivePayIn_shouldAccumulate() {
        // setUp 已缴纳5000，再缴纳3000
        DepositPayInDTO payIn1 = new DepositPayInDTO();
        payIn1.setContractId(CONTRACT_ID);
        payIn1.setAmount(new BigDecimal("3000.00"));
        payIn1.setReason("追加缴纳1");
        depositService.payIn(payIn1);

        // 再缴纳2000
        DepositPayInDTO payIn2 = new DepositPayInDTO();
        payIn2.setContractId(CONTRACT_ID);
        payIn2.setAmount(new BigDecimal("2000.00"));
        payIn2.setReason("追加缴纳2");
        depositService.payIn(payIn2);

        FinDepositAccount account = accountMapper.selectByContractId(CONTRACT_ID);
        assertThat(account.getBalance())
                .as("余额应为5000+3000+2000=10000")
                .isEqualByComparingTo("10000.00");
        assertThat(account.getTotalIn())
                .as("累计缴纳应为10000")
                .isEqualByComparingTo("10000.00");
    }

    // ─── 私有辅助 ──────────────────────────────────────────────────────────────

    private FinReceivable buildReceivable(Long contractId, BigDecimal amount) {
        FinReceivable ar = new FinReceivable();
        ar.setReceivableCode("AR-DEP-" + System.nanoTime());
        ar.setContractId(contractId);
        ar.setMerchantId(MERCHANT_ID);
        ar.setOriginalAmount(amount);
        ar.setAdjustAmount(BigDecimal.ZERO);
        ar.setDeductionAmount(BigDecimal.ZERO);
        ar.setActualAmount(amount);
        ar.setReceivedAmount(BigDecimal.ZERO);
        ar.setOutstandingAmount(amount);
        ar.setDueDate(LocalDate.now().plusDays(30));
        ar.setStatus(0);
        return ar;
    }

    private BigDecimal safeValue(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
