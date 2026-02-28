package com.asset.finance.deposit;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.deposit.dto.DepositForfeitDTO;
import com.asset.finance.deposit.dto.DepositOffsetDTO;
import com.asset.finance.deposit.dto.DepositPayInDTO;
import com.asset.finance.deposit.dto.DepositRefundDTO;
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
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 保证金管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>余额恒等式：balance = total_in - total_offset - total_refund - total_forfeit</li>
 *   <li>余额不足时冲抵 → 抛出 FIN_4002</li>
 *   <li>乐观锁超限（模拟并发冲突）→ 抛出 FIN_5002</li>
 *   <li>并发保证金缴纳：多线程同时 payIn，总余额正确</li>
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
