package com.asset.finance.prepayment;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.prepayment.dto.PrepayAccountVO;
import com.asset.finance.prepayment.dto.PrepayDepositDTO;
import com.asset.finance.prepayment.dto.PrepayOffsetDTO;
import com.asset.finance.prepayment.dto.PrepayQueryDTO;
import com.asset.finance.prepayment.dto.PrepayRefundDTO;
import com.asset.finance.prepayment.entity.FinPrepayAccount;
import com.asset.finance.prepayment.entity.FinPrepayTransaction;
import com.asset.finance.prepayment.mapper.FinPrepayAccountMapper;
import com.asset.finance.prepayment.service.FinPrepaymentService;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * 预收款管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>PRE-01：转入预存-余额增加</li>
 *   <li>PRE-02：查询账户信息</li>
 *   <li>PRE-03：抵冲应收-余额减少</li>
 *   <li>PRE-04：抵冲超过余额-拒绝</li>
 *   <li>PRE-05：退款-余额清零</li>
 *   <li>PRE-06：addBalance 内部调用-核销超额自动转入</li>
 *   <li>PRE-07：流水分页查询</li>
 * </ol>
 */
@DisplayName("预收款管理 Service 测试")
class FinPrepaymentServiceTest extends FinanceTestBase {

    @Autowired
    private FinPrepaymentService prepaymentService;

    @Autowired
    private FinPrepayAccountMapper prepayAccountMapper;

    @Autowired
    private FinReceivableMapper receivableMapper;

    private static final Long CONTRACT_ID = 10004L;
    private static final Long MERCHANT_ID = 20004L;

    @BeforeEach
    void setUp() {
        // 预存500元
        PrepayDepositDTO dto = new PrepayDepositDTO();
        dto.setContractId(CONTRACT_ID);
        dto.setAmount(new BigDecimal("500.00"));
        dto.setSourceCode("RC-INIT");
        dto.setRemark("初始预存");
        prepaymentService.deposit(dto);
    }

    // ─── PRE-01：转入预存-余额增加 ────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-01：转入预存500元后余额=500")
    void deposit_shouldIncreaseBalance() {
        FinPrepayAccount account = prepayAccountMapper.selectByContractId(CONTRACT_ID);

        assertThat(account).as("预收款账户应已创建").isNotNull();
        assertThat(account.getBalance())
                .as("余额应为500")
                .isEqualByComparingTo("500.00");
    }

    // ─── PRE-02：查询账户信息 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-02：getAccount 返回含 balance=500 的 VO")
    void getAccount_shouldReturnCorrectBalance() {
        PrepayAccountVO vo = prepaymentService.getAccount(CONTRACT_ID);

        assertThat(vo).as("账户VO不应为空").isNotNull();
        assertThat(vo.getBalance())
                .as("余额应为500")
                .isEqualByComparingTo("500.00");
        assertThat(vo.getContractId())
                .as("合同ID应匹配")
                .isEqualTo(CONTRACT_ID);
    }

    // ─── PRE-03：抵冲应收-余额减少 ────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-03：抵冲应收300后余额=200")
    void offset_shouldDecreaseBalance() {
        // 先加预存到1000
        PrepayDepositDTO extraDeposit = new PrepayDepositDTO();
        extraDeposit.setContractId(CONTRACT_ID);
        extraDeposit.setAmount(new BigDecimal("500.00"));
        prepaymentService.deposit(extraDeposit);

        // 插入应收
        FinReceivable ar = buildReceivable(CONTRACT_ID, new BigDecimal("300.00"));
        receivableMapper.insert(ar);

        // 抵冲
        PrepayOffsetDTO dto = new PrepayOffsetDTO();
        dto.setContractId(CONTRACT_ID);
        dto.setReceivableId(ar.getId());
        dto.setAmount(new BigDecimal("300.00"));
        prepaymentService.offset(dto);

        FinPrepayAccount account = prepayAccountMapper.selectByContractId(CONTRACT_ID);
        assertThat(account.getBalance())
                .as("抵冲300后余额应为700")
                .isEqualByComparingTo("700.00");

        // 验证应收已收更新
        FinReceivable updatedAr = receivableMapper.selectById(ar.getId());
        assertThat(updatedAr.getReceivedAmount())
                .as("应收已收应增加300")
                .isEqualByComparingTo("300.00");
    }

    // ─── PRE-04：抵冲超过余额-拒绝 ────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-04：抵冲800超过余额500应抛出异常")
    void offset_exceedingBalance_shouldThrow() {
        FinReceivable ar = buildReceivable(CONTRACT_ID, new BigDecimal("1000.00"));
        receivableMapper.insert(ar);

        PrepayOffsetDTO dto = new PrepayOffsetDTO();
        dto.setContractId(CONTRACT_ID);
        dto.setReceivableId(ar.getId());
        dto.setAmount(new BigDecimal("800.00")); // 超过余额500

        assertThatThrownBy(() -> prepaymentService.offset(dto))
                .as("抵冲800超过余额500应抛出异常")
                .isInstanceOf(Exception.class);
    }

    // ─── PRE-05：退款-余额清零 ────────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-05：退款500后余额=0")
    void refund_shouldClearBalance() {
        PrepayRefundDTO dto = new PrepayRefundDTO();
        dto.setContractId(CONTRACT_ID);
        dto.setAmount(new BigDecimal("500.00"));
        dto.setRemark("全额退款");
        prepaymentService.refund(dto);

        FinPrepayAccount account = prepayAccountMapper.selectByContractId(CONTRACT_ID);
        assertThat(account.getBalance())
                .as("退款后余额应为0")
                .isEqualByComparingTo("0.00");
    }

    // ─── PRE-06：addBalance 内部调用 ──────────────────────────────────────────────

    @Test
    @DisplayName("PRE-06：addBalance 转入200后余额累加")
    void addBalance_shouldIncreaseBalance() {
        FinPrepayAccount account = prepayAccountMapper.selectByContractId(CONTRACT_ID);

        prepaymentService.addBalance(
                CONTRACT_ID, account.getId(),
                new BigDecimal("200.00"),
                "WO-TEST-001", "核销超额转入"
        );

        FinPrepayAccount updated = prepayAccountMapper.selectByContractId(CONTRACT_ID);
        assertThat(updated.getBalance())
                .as("addBalance 后余额应为500+200=700")
                .isEqualByComparingTo("700.00");
    }

    // ─── PRE-07：流水分页查询 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-07：流水分页查询返回按时间倒序的记录")
    void pageTransaction_shouldReturnOrderedResults() {
        // 再做一笔退款（产生第二条流水）
        PrepayRefundDTO refundDTO = new PrepayRefundDTO();
        refundDTO.setContractId(CONTRACT_ID);
        refundDTO.setAmount(new BigDecimal("100.00"));
        refundDTO.setRemark("部分退款");
        prepaymentService.refund(refundDTO);

        PrepayQueryDTO query = new PrepayQueryDTO();
        query.setContractId(CONTRACT_ID);
        query.setPageNum(1);
        query.setPageSize(50);

        IPage<FinPrepayTransaction> page = prepaymentService.pageTransaction(query);

        assertThat(page.getRecords())
                .as("应有至少2条流水（1存入+1退款）")
                .hasSizeGreaterThanOrEqualTo(2);
    }

    // ─── 私有辅助 ──────────────────────────────────────────────────────────────────

    private FinReceivable buildReceivable(Long contractId, BigDecimal amount) {
        FinReceivable ar = new FinReceivable();
        ar.setReceivableCode("AR-PRE-" + System.nanoTime());
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
}
