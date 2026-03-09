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

import com.asset.finance.receipt.dto.WriteOffDetailVO;
import com.asset.finance.receipt.dto.WritableReceivableVO;
import com.asset.finance.receipt.entity.FinWriteOffDetail;
import com.asset.finance.receipt.mapper.FinWriteOffDetailMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 核销管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>WOF-01：精确核销：收款1000 核销1000 → outstanding=0，receipt.status=2</li>
 *   <li>WOF-02：超额转预存：收款1200，应收1000 → prepay_account.balance=200</li>
 *   <li>WOF-03：负数核销方向校验：正金额提交负数核销 → FIN_4005</li>
 *   <li>WOF-04：超出收款余额校验 → FIN_4001</li>
 *   <li>WOF-05：部分核销-应收状态变为部分收款(1)</li>
 *   <li>WOF-06：多笔应收同时核销</li>
 *   <li>WOF-07：查询可核销应收列表</li>
 *   <li>WOF-08：核销单作废-仅待审核可作废</li>
 *   <li>WOF-09：审批驳回-核销单状态变为驳回(2)</li>
 *   <li>WOF-10：连续两次核销同一收款单</li>
 *   <li>WOF-11：核销详情查询-含明细行</li>
 *   <li>WOF-12：已通过核销单不可作废</li>
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
    private FinWriteOffDetailMapper writeOffDetailMapper;

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

    // ─── WOF-05：部分核销 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-05：部分核销500，应收status=1，outstanding=500，receipt.status=1")
    void partialWriteOff_shouldSetStatusToPartial() {
        // 收款500
        FinReceipt receipt500 = buildReceipt(new BigDecimal("500.00"), 0);
        receiptMapper.insert(receipt500);

        Long writeOffId = writeOffService.submitWriteOff(
                receipt500.getId(),
                List.of(item(savedReceivable.getId(), "500.00")),
                1
        );
        String approvalId = "MOCK-APPROVAL-" + writeOffId;
        updateWriteOffApprovalId(writeOffId, approvalId);
        writeOffService.approveCallback(approvalId, true, "同意");

        FinReceivable ar = receivableMapper.selectById(savedReceivable.getId());
        assertThat(ar.getReceivedAmount())
                .as("已收金额应为500")
                .isEqualByComparingTo("500.00");
        assertThat(ar.getOutstandingAmount())
                .as("欠费应为500")
                .isEqualByComparingTo("500.00");
        assertThat(ar.getStatus())
                .as("应收状态应为部分收款(1)")
                .isEqualTo(1);

        FinReceipt updatedReceipt = receiptMapper.selectById(receipt500.getId());
        assertThat(updatedReceipt.getStatus())
                .as("收款单状态应为已全部核销(2)")
                .isEqualTo(2);
    }

    // ─── WOF-06：多笔应收同时核销 ────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-06：收款2000，同时核销应收A=800和应收B=700")
    void multiReceivableWriteOff_shouldSettleBoth() {
        // 收款2000
        FinReceipt receipt2000 = buildReceipt(new BigDecimal("2000.00"), 0);
        receiptMapper.insert(receipt2000);

        // 应收A 800元
        FinReceivable arA = buildReceivable(new BigDecimal("800.00"));
        receivableMapper.insert(arA);
        // 应收B 700元
        FinReceivable arB = buildReceivable(new BigDecimal("700.00"));
        receivableMapper.insert(arB);

        Long writeOffId = writeOffService.submitWriteOff(
                receipt2000.getId(),
                List.of(item(arA.getId(), "800.00"), item(arB.getId(), "700.00")),
                1
        );
        String approvalId = "MOCK-APPROVAL-" + writeOffId;
        updateWriteOffApprovalId(writeOffId, approvalId);
        writeOffService.approveCallback(approvalId, true, "同意");

        FinReceivable updatedA = receivableMapper.selectById(arA.getId());
        assertThat(updatedA.getOutstandingAmount())
                .as("应收A欠费应为0")
                .isEqualByComparingTo("0.00");
        assertThat(updatedA.getStatus())
                .as("应收A应为已收清(2)")
                .isEqualTo(2);

        FinReceivable updatedB = receivableMapper.selectById(arB.getId());
        assertThat(updatedB.getOutstandingAmount())
                .as("应收B欠费应为0")
                .isEqualByComparingTo("0.00");

        FinReceipt updatedReceipt = receiptMapper.selectById(receipt2000.getId());
        assertThat(updatedReceipt.getWriteOffAmount())
                .as("收款单核销金额应为1500")
                .isEqualByComparingTo("1500.00");
    }

    // ─── WOF-07：查询可核销应收列表 ──────────────────────────────────────────────

    @Test
    @DisplayName("WOF-07：queryWritableReceivables 只返回 status in (0,1)")
    void queryWritableReceivables_shouldExcludeFullyPaid() {
        // savedReceivable status=0
        // 插入 status=1 的应收
        FinReceivable arPartial = buildReceivable(new BigDecimal("500.00"));
        arPartial.setStatus(1);
        arPartial.setReceivedAmount(new BigDecimal("200.00"));
        arPartial.setOutstandingAmount(new BigDecimal("300.00"));
        receivableMapper.insert(arPartial);

        // 插入 status=2 的应收（已收清，不应返回）
        FinReceivable arPaid = buildReceivable(new BigDecimal("300.00"));
        arPaid.setStatus(2);
        arPaid.setReceivedAmount(new BigDecimal("300.00"));
        arPaid.setOutstandingAmount(BigDecimal.ZERO);
        receivableMapper.insert(arPaid);

        List<WritableReceivableVO> list = writeOffService.queryWritableReceivables(CONTRACT_ID);

        assertThat(list)
                .as("应返回 status=0 和 status=1 的记录")
                .hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(vo -> assertThat(vo.getStatus()).isIn(0, 1));
    }

    // ─── WOF-08：核销单作废-仅待审核可作废 ────────────────────────────────────────

    @Test
    @DisplayName("WOF-08：待审核的核销单可以作废")
    void cancelWriteOff_pendingStatus_shouldSucceed() {
        Long writeOffId = writeOffService.submitWriteOff(
                savedReceipt.getId(),
                List.of(item(savedReceivable.getId(), "500.00")),
                1
        );

        writeOffService.cancelWriteOff(writeOffId);

        FinWriteOff wo = writeOffMapper.selectById(writeOffId);
        assertThat(wo.getStatus())
                .as("作废后状态应为2(已撤销)")
                .isEqualTo(2);

        // 收款单余额不受影响
        FinReceipt receipt = receiptMapper.selectById(savedReceipt.getId());
        assertThat(receipt.getWriteOffAmount())
                .as("收款单核销金额应不变(0)")
                .isEqualByComparingTo("0.00");
    }

    // ─── WOF-09：审批驳回 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-09：审批驳回后核销单status=2，应收/收款金额不变")
    void approveCallback_rejected_shouldNotChangeAmounts() {
        Long writeOffId = writeOffService.submitWriteOff(
                savedReceipt.getId(),
                List.of(item(savedReceivable.getId(), "800.00")),
                1
        );
        String approvalId = "MOCK-APPROVAL-" + writeOffId;
        updateWriteOffApprovalId(writeOffId, approvalId);

        writeOffService.approveCallback(approvalId, false, "不同意");

        FinWriteOff wo = writeOffMapper.selectById(writeOffId);
        assertThat(wo.getStatus())
                .as("驳回后状态应为2")
                .isEqualTo(2);

        FinReceivable ar = receivableMapper.selectById(savedReceivable.getId());
        assertThat(ar.getReceivedAmount())
                .as("驳回后应收已收金额应仍为0")
                .isEqualByComparingTo("0.00");

        FinReceipt receipt = receiptMapper.selectById(savedReceipt.getId());
        assertThat(receipt.getWriteOffAmount())
                .as("驳回后收款单核销金额应仍为0")
                .isEqualByComparingTo("0.00");
    }

    // ─── WOF-10：连续两次核销同一收款单 ──────────────────────────────────────────

    @Test
    @DisplayName("WOF-10：收款2000，第1次核销800 第2次核销200，receivable.outstanding=0")
    void consecutiveWriteOff_shouldAccumulate() {
        // 收款2000
        FinReceipt receipt2000 = buildReceipt(new BigDecimal("2000.00"), 0);
        receiptMapper.insert(receipt2000);

        // 第1次核销800
        Long wo1 = writeOffService.submitWriteOff(
                receipt2000.getId(),
                List.of(item(savedReceivable.getId(), "800.00")),
                1
        );
        String appr1 = "MOCK-APPROVAL-" + wo1;
        updateWriteOffApprovalId(wo1, appr1);
        writeOffService.approveCallback(appr1, true, "同意");

        // 第2次核销200
        Long wo2 = writeOffService.submitWriteOff(
                receipt2000.getId(),
                List.of(item(savedReceivable.getId(), "200.00")),
                1
        );
        String appr2 = "MOCK-APPROVAL-WO2-" + wo2;
        updateWriteOffApprovalId(wo2, appr2);
        writeOffService.approveCallback(appr2, true, "同意");

        FinReceivable ar = receivableMapper.selectById(savedReceivable.getId());
        assertThat(ar.getReceivedAmount())
                .as("累计已收应为1000")
                .isEqualByComparingTo("1000.00");
        assertThat(ar.getOutstandingAmount())
                .as("欠费应为0")
                .isEqualByComparingTo("0.00");
        assertThat(ar.getStatus())
                .as("应为已收清(2)")
                .isEqualTo(2);

        FinReceipt updatedReceipt = receiptMapper.selectById(receipt2000.getId());
        assertThat(updatedReceipt.getWriteOffAmount())
                .as("收款单核销金额应为1000")
                .isEqualByComparingTo("1000.00");
        assertThat(updatedReceipt.getStatus())
                .as("收款单状态应为部分核销(1)")
                .isEqualTo(1);
    }

    // ─── WOF-11：核销详情查询-含明细行 ────────────────────────────────────────────

    @Test
    @DisplayName("WOF-11：getDetailById 应返回含明细行的核销详情")
    void getDetailById_shouldContainDetails() {
        Long writeOffId = writeOffService.submitWriteOff(
                savedReceipt.getId(),
                List.of(item(savedReceivable.getId(), "1000.00")),
                1
        );
        String approvalId = "MOCK-APPROVAL-" + writeOffId;
        updateWriteOffApprovalId(writeOffId, approvalId);
        writeOffService.approveCallback(approvalId, true, "同意");

        WriteOffDetailVO vo = writeOffService.getDetailById(writeOffId);

        assertThat(vo).as("详情VO不应为空").isNotNull();
        assertThat(vo.getWriteOffCode()).as("核销编号应以WO-开头").startsWith("WO-");
        assertThat(vo.getDetails())
                .as("详情应包含1条明细行")
                .hasSize(1);
        assertThat(vo.getDetails().get(0).getReceivableId())
                .as("明细行应关联正确的应收ID")
                .isEqualTo(savedReceivable.getId());
    }

    // ─── WOF-12：已通过核销单不可作废 ────────────────────────────────────────────

    @Test
    @DisplayName("WOF-12：已审批通过的核销单不可作废")
    void cancelWriteOff_approvedStatus_shouldThrow() {
        Long writeOffId = writeOffService.submitWriteOff(
                savedReceipt.getId(),
                List.of(item(savedReceivable.getId(), "1000.00")),
                1
        );
        String approvalId = "MOCK-APPROVAL-" + writeOffId;
        updateWriteOffApprovalId(writeOffId, approvalId);
        writeOffService.approveCallback(approvalId, true, "同意");

        assertThatThrownBy(() -> writeOffService.cancelWriteOff(writeOffId))
                .as("已通过核销单不可作废，应抛出异常")
                .isInstanceOf(Exception.class);
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
