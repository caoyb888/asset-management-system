package com.asset.finance.receipt;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.prepayment.entity.FinPrepayAccount;
import com.asset.finance.prepayment.mapper.FinPrepayAccountMapper;
import com.asset.finance.receipt.dto.WriteOffDetailItemDTO;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinWriteOff;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.mapper.FinWriteOffMapper;
import com.asset.finance.receipt.service.FinWriteOffService;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 核销管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>精确核销：收款1000 核销1000 → outstanding=0，receipt.status=2</li>
 *   <li>超额转预存：收款1200，应收1000 → prepay_account.balance=200</li>
 *   <li>负数核销方向校验：正金额提交负数核销 → FIN_4005</li>
 *   <li>超出收款余额校验 → FIN_4001</li>
 * </ol>
 */
@DisplayName("核销管理 Service 测试")
class FinWriteOffServiceTest extends FinanceTestBase {

    @Autowired
    private FinWriteOffService writeOffService;

    @Autowired
    private FinReceiptMapper receiptMapper;

    @Autowired
    private FinReceivableMapper receivableMapper;

    @Autowired
    private FinWriteOffMapper writeOffMapper;

    @Autowired
    private FinPrepayAccountMapper prepayAccountMapper;

    /** 基础合同ID，测试中不做跨模块联查 */
    private static final Long CONTRACT_ID = 10001L;
    private static final Long MERCHANT_ID = 20001L;
    private static final Long PROJECT_ID  = 30001L;

    private FinReceipt savedReceipt;
    private FinReceivable savedReceivable;

    @BeforeEach
    void setUp() {
        // 插入测试收款单（1000元，待核销）
        savedReceipt = buildReceipt(new BigDecimal("1000.00"), 0);
        receiptMapper.insert(savedReceipt);

        // 插入测试应收记录（1000元，待收）
        savedReceivable = buildReceivable(new BigDecimal("1000.00"));
        receivableMapper.insert(savedReceivable);
    }

    // ─── 场景1：精确核销 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景1：核销1000，应收1000 → outstanding=0，receipt.status=2")
    void exactWriteOff_shouldFullySettleReceivableAndReceipt() {
        // 提交核销
        Long writeOffId = writeOffService.submitWriteOff(
                savedReceipt.getId(),
                List.of(item(savedReceivable.getId(), "1000.00")),
                1
        );

        // 模拟OA审批通过
        String approvalId = "MOCK-APPROVAL-" + writeOffId;
        updateWriteOffApprovalId(writeOffId, approvalId);
        writeOffService.approveCallback(approvalId, true, "同意");

        // 验证应收记录
        FinReceivable updatedReceivable = receivableMapper.selectById(savedReceivable.getId());
        assertThat(updatedReceivable.getReceivedAmount())
                .as("已收金额应为1000")
                .isEqualByComparingTo("1000.00");
        assertThat(updatedReceivable.getOutstandingAmount())
                .as("欠费金额应为0")
                .isEqualByComparingTo("0.00");
        assertThat(updatedReceivable.getStatus())
                .as("应收状态应为已收清(2)")
                .isEqualTo(2);

        // 验证收款单
        FinReceipt updatedReceipt = receiptMapper.selectById(savedReceipt.getId());
        assertThat(updatedReceipt.getWriteOffAmount())
                .as("核销金额应为1000")
                .isEqualByComparingTo("1000.00");
        assertThat(updatedReceipt.getStatus())
                .as("收款单状态应为已全部核销(2)")
                .isEqualTo(2);
    }

    // ─── 场景2：超额转预存款 ───────────────────────────────────────────────────

    @Test
    @DisplayName("场景2：收款1200，应收1000核销 → prepay_account.balance=200")
    void overwriteOff_shouldTransferOverpayToPrepayAccount() {
        // 插入1200元收款单
        FinReceipt receipt1200 = buildReceipt(new BigDecimal("1200.00"), 0);
        receiptMapper.insert(receipt1200);

        // 核销1200对应1000的应收（超出200元）
        Long writeOffId = writeOffService.submitWriteOff(
                receipt1200.getId(),
                List.of(item(savedReceivable.getId(), "1200.00")),
                1
        );
        String approvalId = "MOCK-APPROVAL-" + writeOffId;
        updateWriteOffApprovalId(writeOffId, approvalId);
        writeOffService.approveCallback(approvalId, true, "同意");

        // 验证预收款账户余额 = 200
        FinPrepayAccount prepayAccount = prepayAccountMapper.selectByContractId(CONTRACT_ID);
        assertThat(prepayAccount).as("预收款账户应已创建").isNotNull();
        assertThat(prepayAccount.getBalance())
                .as("预存余额应为200")
                .isEqualByComparingTo("200.00");

        // 验证应收已收清
        FinReceivable ar = receivableMapper.selectById(savedReceivable.getId());
        assertThat(ar.getOutstandingAmount()).isEqualByComparingTo("0.00");
        assertThat(ar.getStatus()).isEqualTo(2);
    }

    // ─── 场景3：负数核销方向校验 ──────────────────────────────────────────────

    @Test
    @DisplayName("场景3：正金额提交负数核销 → 抛出 FIN_4005")
    void negativeWriteOff_withPositiveAmount_shouldThrowFIN4005() {
        assertThatThrownBy(() ->
                writeOffService.submitWriteOff(
                        savedReceipt.getId(),
                        List.of(item(savedReceivable.getId(), "500.00")),
                        4  // 负数核销类型
                )
        )
        .isInstanceOf(FinBizException.class)
        .extracting(e -> ((FinBizException) e).getCode())
        .isEqualTo(FinErrorCode.FIN_4005.getCode());
    }

    // ─── 场景4：核销金额超出收款余额 ──────────────────────────────────────────

    @Test
    @DisplayName("场景4：核销金额超出收款余额 → 抛出 FIN_4001")
    void writeOff_exceedReceiptBalance_shouldThrowFIN4001() {
        assertThatThrownBy(() ->
                writeOffService.submitWriteOff(
                        savedReceipt.getId(),
                        List.of(item(savedReceivable.getId(), "2000.00")),  // 超出1000
                        1
                )
        )
        .isInstanceOf(FinBizException.class)
        .extracting(e -> ((FinBizException) e).getCode())
        .isEqualTo(FinErrorCode.FIN_4001.getCode());
    }

    // ─── 私有构建方法 ──────────────────────────────────────────────────────────

    private FinReceipt buildReceipt(BigDecimal amount, int status) {
        FinReceipt r = new FinReceipt();
        r.setReceiptCode("RC-TEST-" + System.nanoTime());
        r.setContractId(CONTRACT_ID);
        r.setMerchantId(MERCHANT_ID);
        r.setProjectId(PROJECT_ID);
        r.setTotalAmount(amount);
        r.setPaymentMethod(1);
        r.setReceiptDate(LocalDate.now());
        r.setStatus(status);
        r.setWriteOffAmount(BigDecimal.ZERO);
        r.setPrepayAmount(BigDecimal.ZERO);
        return r;
    }

    private FinReceivable buildReceivable(BigDecimal amount) {
        FinReceivable ar = new FinReceivable();
        ar.setReceivableCode("AR-TEST-" + System.nanoTime());
        ar.setContractId(CONTRACT_ID);
        ar.setMerchantId(MERCHANT_ID);
        ar.setProjectId(PROJECT_ID);
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

    private WriteOffDetailItemDTO item(Long receivableId, String amount) {
        WriteOffDetailItemDTO dto = new WriteOffDetailItemDTO();
        dto.setReceivableId(receivableId);
        dto.setWriteOffAmount(new BigDecimal(amount));
        return dto;
    }

    /**
     * 由于 submitWriteOff 内部 OA 提交失败会 warn 级别跳过，
     * 测试中直接将 approvalId 写入数据库，模拟 OA 回调场景
     */
    private void updateWriteOffApprovalId(Long writeOffId, String approvalId) {
        FinWriteOff wo = writeOffMapper.selectById(writeOffId);
        wo.setApprovalId(approvalId);
        writeOffMapper.updateById(wo);
    }
}
