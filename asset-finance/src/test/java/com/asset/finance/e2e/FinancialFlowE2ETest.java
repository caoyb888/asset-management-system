package com.asset.finance.e2e;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.deposit.dto.DepositOffsetDTO;
import com.asset.finance.deposit.dto.DepositPayInDTO;
import com.asset.finance.deposit.service.FinDepositService;
import com.asset.finance.prepayment.service.FinPrepaymentService;
import com.asset.finance.receipt.dto.ReceiptCreateDTO;
import com.asset.finance.receipt.dto.ReceiptDetailItemDTO;
import com.asset.finance.receipt.dto.WriteOffDetailItemDTO;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinWriteOff;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.mapper.FinWriteOffMapper;
import com.asset.finance.receipt.service.FinReceiptService;
import com.asset.finance.receipt.service.FinWriteOffService;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import com.asset.finance.voucher.entity.FinVoucher;
import com.asset.finance.voucher.mapper.FinVoucherMapper;
import com.asset.finance.voucher.service.FinVoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 核心业务场景端到端验证
 *
 * <p>跨越多个 Service，验证完整资金流转闭环。
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>E2E-01：收款→核销→应收完整闭环</li>
 *   <li>E2E-02：超额核销→预存→抵冲</li>
 *   <li>E2E-03：保证金冲抵应收</li>
 *   <li>E2E-04：核销→生成凭证→审核→上传</li>
 *   <li>E2E-05：减免+调整+核销综合</li>
 * </ol>
 */
@DisplayName("核心业务场景端到端验证")
class FinancialFlowE2ETest extends FinanceTestBase {

    @Autowired private FinReceivableMapper receivableMapper;
    @Autowired private FinReceiptMapper receiptMapper;
    @Autowired private FinWriteOffMapper writeOffMapper;
    @Autowired private FinVoucherMapper voucherMapper;

    @Autowired private FinReceiptService receiptService;
    @Autowired private FinWriteOffService writeOffService;
    @Autowired private FinDepositService depositService;
    @Autowired private FinPrepaymentService prepaymentService;
    @Autowired private FinVoucherService voucherService;
    @Autowired private com.asset.finance.receivable.service.FinReceivableService receivableService;

    private static final Long CONTRACT_ID = 99001L;
    private static final Long MERCHANT_ID = 88001L;

    // ─── 辅助方法 ──────────────────────────────────────────────────────────────

    private FinReceivable createReceivable(BigDecimal amount) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-E2E-" + System.nanoTime());
        r.setContractId(CONTRACT_ID);
        r.setMerchantId(MERCHANT_ID);
        r.setOriginalAmount(amount);
        r.setAdjustAmount(BigDecimal.ZERO);
        r.setDeductionAmount(BigDecimal.ZERO);
        r.setActualAmount(amount);
        r.setReceivedAmount(BigDecimal.ZERO);
        r.setOutstandingAmount(amount);
        r.setDueDate(LocalDate.now().minusDays(5));
        r.setStatus(0);
        r.setAccrualMonth(LocalDate.now().toString().substring(0, 7));
        receivableMapper.insert(r);
        return r;
    }

    private Long createReceipt(BigDecimal amount) {
        ReceiptCreateDTO dto = new ReceiptCreateDTO();
        dto.setContractId(CONTRACT_ID);
        dto.setTotalAmount(amount);
        dto.setReceiptDate(LocalDate.now());
        dto.setPaymentMethod(1);
        return receiptService.create(dto);
    }

    private Long submitAndApproveWriteOff(Long receiptId, Long receivableId, BigDecimal amount) {
        WriteOffDetailItemDTO item = new WriteOffDetailItemDTO();
        item.setReceivableId(receivableId);
        item.setWriteOffAmount(amount);

        Long writeOffId = writeOffService.submitWriteOff(receiptId, List.of(item), 1);
        FinWriteOff wo = writeOffMapper.selectById(writeOffId);
        writeOffService.approveCallback(wo.getApprovalId(), true, "E2E通过");
        return writeOffId;
    }

    // ─── E2E-01：收款→核销→应收完整闭环 ────────────────────────────────────────

    @Test
    @DisplayName("E2E-01：收款1000→核销→应收outstanding=0/status=2，收款writeOff=1000/status=2")
    void fullWriteOffCycle() {
        // ① 创建应收1000
        FinReceivable ar = createReceivable(new BigDecimal("1000.00"));

        // ② 创建收款单1000
        Long receiptId = createReceipt(new BigDecimal("1000.00"));

        // ③④ 提交核销 + 审批通过
        submitAndApproveWriteOff(receiptId, ar.getId(), new BigDecimal("1000.00"));

        // 验证应收
        FinReceivable updatedAr = receivableMapper.selectById(ar.getId());
        assertThat(updatedAr.getOutstandingAmount())
                .as("应收 outstanding 应为0")
                .isEqualByComparingTo("0.00");
        assertThat(updatedAr.getStatus())
                .as("应收 status 应为2(已收清)")
                .isEqualTo(2);

        // 验证收款单
        FinReceipt updatedReceipt = receiptMapper.selectById(receiptId);
        assertThat(updatedReceipt.getWriteOffAmount())
                .as("收款单 writeOffAmount 应为1000")
                .isEqualByComparingTo("1000.00");
        assertThat(updatedReceipt.getStatus())
                .as("收款单 status 应为2(已全部核销)")
                .isEqualTo(2);
    }

    // ─── E2E-02：超额核销→预存→抵冲 ────────────────────────────────────────────

    @Test
    @DisplayName("E2E-02：收款1200核销应收1000(超额200转预存)→预存抵冲应收B 150→余额50")
    void overPaymentToPrePayAndOffset() {
        // ① 创建应收A=1000
        FinReceivable arA = createReceivable(new BigDecimal("1000.00"));

        // ② 创建收款1200
        Long receiptId = createReceipt(new BigDecimal("1200.00"));

        // ③ 核销1200（审批通过），其中1000冲抵应收A，超额200转预存
        submitAndApproveWriteOff(receiptId, arA.getId(), new BigDecimal("1200.00"));

        // 验证应收A已收清
        FinReceivable updatedA = receivableMapper.selectById(arA.getId());
        assertThat(updatedA.getStatus()).as("应收A status=2").isEqualTo(2);

        // 验证预存余额（收款单prepayAmount应为200）
        FinReceipt updatedReceipt = receiptMapper.selectById(receiptId);
        assertThat(updatedReceipt.getPrepayAmount())
                .as("收款单 prepayAmount 应为200")
                .isEqualByComparingTo("200.00");

        // ④ 创建应收B=150
        FinReceivable arB = createReceivable(new BigDecimal("150.00"));

        // ⑤ 预存抵冲150
        com.asset.finance.prepayment.dto.PrepayOffsetDTO offsetDTO =
                new com.asset.finance.prepayment.dto.PrepayOffsetDTO();
        offsetDTO.setContractId(CONTRACT_ID);
        offsetDTO.setReceivableId(arB.getId());
        offsetDTO.setAmount(new BigDecimal("150.00"));
        prepaymentService.offset(offsetDTO);

        // 验证应收B
        FinReceivable updatedB = receivableMapper.selectById(arB.getId());
        assertThat(updatedB.getReceivedAmount())
                .as("应收B receivedAmount 应为150")
                .isEqualByComparingTo("150.00");

        // 验证预存余额为50
        assertThat(prepaymentService.getAccount(CONTRACT_ID).getBalance())
                .as("预存余额应为50")
                .isEqualByComparingTo("50.00");
    }

    // ─── E2E-03：保证金冲抵应收 ─────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-03：保证金缴纳5000→冲抵应收3000→审批通过→余额2000")
    void depositOffsetReceivable() {
        // ① 缴纳保证金5000
        DepositPayInDTO payInDTO = new DepositPayInDTO();
        payInDTO.setContractId(CONTRACT_ID);
        payInDTO.setAmount(new BigDecimal("5000.00"));
        payInDTO.setSourceCode("RC-E2E-001");
        depositService.payIn(payInDTO);

        // ② 创建应收3000
        FinReceivable ar = createReceivable(new BigDecimal("3000.00"));

        // ③ 申请冲抵3000
        DepositOffsetDTO offsetDTO = new DepositOffsetDTO();
        offsetDTO.setContractId(CONTRACT_ID);
        offsetDTO.setReceivableId(ar.getId());
        offsetDTO.setAmount(new BigDecimal("3000.00"));
        Long transId = depositService.processOffset(offsetDTO);

        // ④ 审批通过（mock approvalId 格式为 "MOCK-APPROVAL-{transId}"）
        String approvalId = "MOCK-APPROVAL-" + transId;
        depositService.approveCallback(approvalId, true);

        // 验证
        assertThat(depositService.getAccount(CONTRACT_ID).getBalance())
                .as("保证金余额应为2000")
                .isEqualByComparingTo("2000.00");

        FinReceivable updatedAr = receivableMapper.selectById(ar.getId());
        assertThat(updatedAr.getReceivedAmount())
                .as("应收 receivedAmount 应为3000")
                .isEqualByComparingTo("3000.00");
    }

    // ─── E2E-04：核销→生成凭证→审核→上传 ────────────────────────────────────────

    @Test
    @DisplayName("E2E-04：核销→生成凭证→审核→上传，凭证status=2，借贷平衡")
    void writeOffToVoucherPipeline() {
        // ① 创建应收+收款
        FinReceivable ar = createReceivable(new BigDecimal("2000.00"));
        Long receiptId = createReceipt(new BigDecimal("2000.00"));

        // ② 核销
        submitAndApproveWriteOff(receiptId, ar.getId(), new BigDecimal("2000.00"));

        // ③ 从收款单生成凭证
        Long voucherId = voucherService.generateFromReceipt(receiptId);

        // ④ 审核
        voucherService.audit(voucherId);

        // ⑤ 上传
        voucherService.upload(voucherId);

        FinVoucher voucher = voucherMapper.selectById(voucherId);
        assertThat(voucher.getStatus()).as("凭证 status 应为2").isEqualTo(2);
        assertThat(voucher.getUploadTime()).as("uploadTime 非空").isNotNull();
        assertThat(voucher.getTotalDebit())
                .as("借贷平衡")
                .isEqualByComparingTo(voucher.getTotalCredit());
    }

    // ─── E2E-05：减免+调整+核销综合 ─────────────────────────────────────────────

    @Test
    @DisplayName("E2E-05：应收1000→调增300→减免200→actual=1100→核销1100→outstanding=0")
    void adjustmentDeductionAndWriteOff() {
        // ① 创建应收 original=1000
        FinReceivable ar = createReceivable(new BigDecimal("1000.00"));

        // ② 调增300
        com.asset.finance.receivable.dto.AdjustmentCreateDTO adjDTO =
                new com.asset.finance.receivable.dto.AdjustmentCreateDTO();
        adjDTO.setReceivableId(ar.getId());
        adjDTO.setAdjustType(1); // 调增
        adjDTO.setAdjustAmount(new BigDecimal("300.00"));
        adjDTO.setReason("E2E调增");
        Long adjId = receivableService.applyAdjustment(adjDTO);
        // 审批回调（mock approvalId = "MOCK-APPROVAL-{adjId}"）
        receivableService.adjustmentCallback("MOCK-APPROVAL-" + adjId, true);

        // ③ 减免200
        com.asset.finance.receivable.dto.DeductionCreateDTO dedDTO =
                new com.asset.finance.receivable.dto.DeductionCreateDTO();
        dedDTO.setReceivableId(ar.getId());
        dedDTO.setDeductionAmount(new BigDecimal("200.00"));
        dedDTO.setReason("E2E减免");
        Long dedId = receivableService.applyDeduction(dedDTO);
        receivableService.deductionCallback("MOCK-APPROVAL-" + dedId, true);

        // 验证 actual = 1000 + 300 - 200 = 1100
        FinReceivable afterAdj = receivableMapper.selectById(ar.getId());
        assertThat(afterAdj.getActualAmount())
                .as("actual 应为1100")
                .isEqualByComparingTo("1100.00");
        assertThat(afterAdj.getOriginalAmount())
                .as("original 应保持1000不变")
                .isEqualByComparingTo("1000.00");

        // ④ 收款1100
        Long receiptId = createReceipt(new BigDecimal("1100.00"));

        // ⑤ 核销1100
        submitAndApproveWriteOff(receiptId, ar.getId(), new BigDecimal("1100.00"));

        FinReceivable finalAr = receivableMapper.selectById(ar.getId());
        assertThat(finalAr.getOutstandingAmount())
                .as("outstanding 应为0")
                .isEqualByComparingTo("0.00");
        assertThat(finalAr.getStatus())
                .as("status 应为2(已收清)")
                .isEqualTo(2);
    }
}
