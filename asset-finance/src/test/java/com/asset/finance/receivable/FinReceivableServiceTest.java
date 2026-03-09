package com.asset.finance.receivable;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.receivable.dto.*;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.entity.FinReceivableAdjustment;
import com.asset.finance.receivable.entity.FinReceivableDeduction;
import com.asset.finance.receivable.mapper.FinReceivableAdjustmentMapper;
import com.asset.finance.receivable.mapper.FinReceivableDeductionMapper;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import com.asset.finance.receivable.service.FinReceivableService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 应收管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>RCV-01：减免审批通过后 original_amount 不变，deduction_amount 正确累积</li>
 *   <li>RCV-02：actual = original + adjust - deduction 公式正确</li>
 *   <li>RCV-03：pageQuery 按 contractId 筛选</li>
 *   <li>RCV-04：pageQuery 按 status 筛选</li>
 *   <li>RCV-05：summaryByContract 多费项汇总</li>
 *   <li>RCV-06：overdueStatistics 三档分布</li>
 *   <li>RCV-07：减免超过欠费额被拒绝</li>
 *   <li>RCV-08：减免驳回不影响金额</li>
 *   <li>RCV-09：adjustType=2（调减）adjust_amount 累积为负数</li>
 *   <li>RCV-10：markPrinted 批量标记</li>
 *   <li>RCV-11：连续两次减免累积 deduction_amount</li>
 *   <li>RCV-12：全额减免 → status=3</li>
 * </ol>
 */
@DisplayName("应收管理 Service 测试")
class FinReceivableServiceTest extends FinanceTestBase {

    @Autowired
    private FinReceivableService receivableService;

    @Autowired
    private FinReceivableMapper receivableMapper;

    @Autowired
    private FinReceivableDeductionMapper deductionMapper;

    @Autowired
    private FinReceivableAdjustmentMapper adjustmentMapper;

    private static final Long CONTRACT_ID = 10002L;
    private static final Long CONTRACT_ID_2 = 10003L;
    private static final Long MERCHANT_ID = 20002L;
    private static final Long PROJECT_ID = 1L;

    private FinReceivable savedAr;

    @BeforeEach
    void setUp() {
        // 应收：原始1000元，未收任何款项
        savedAr = new FinReceivable();
        savedAr.setReceivableCode("AR-RCV-" + System.nanoTime());
        savedAr.setContractId(CONTRACT_ID);
        savedAr.setProjectId(PROJECT_ID);
        savedAr.setMerchantId(MERCHANT_ID);
        savedAr.setOriginalAmount(new BigDecimal("1000.00"));
        savedAr.setAdjustAmount(BigDecimal.ZERO);
        savedAr.setDeductionAmount(BigDecimal.ZERO);
        savedAr.setActualAmount(new BigDecimal("1000.00"));
        savedAr.setReceivedAmount(BigDecimal.ZERO);
        savedAr.setOutstandingAmount(new BigDecimal("1000.00"));
        savedAr.setDueDate(LocalDate.now().plusDays(30));
        savedAr.setStatus(0);
        receivableMapper.insert(savedAr);
    }

    // ─── 辅助方法 ────────────────────────────────────────────────────────────────

    /** 创建并插入一条应收记录 */
    private FinReceivable insertReceivable(Long contractId, Long merchantId, BigDecimal original,
                                           LocalDate dueDate, int status) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-RCV-" + System.nanoTime());
        r.setContractId(contractId);
        r.setProjectId(PROJECT_ID);
        r.setMerchantId(merchantId);
        r.setOriginalAmount(original);
        r.setAdjustAmount(BigDecimal.ZERO);
        r.setDeductionAmount(BigDecimal.ZERO);
        r.setActualAmount(original);
        r.setReceivedAmount(BigDecimal.ZERO);
        r.setOutstandingAmount(original);
        r.setDueDate(dueDate);
        r.setStatus(status);
        receivableMapper.insert(r);
        return r;
    }

    /** 申请减免并模拟审批通过/驳回 */
    private void applyAndCallbackDeduction(Long receivableId, BigDecimal amount, boolean approved) {
        DeductionCreateDTO dto = new DeductionCreateDTO();
        dto.setReceivableId(receivableId);
        dto.setDeductionAmount(amount);
        dto.setReason("测试减免");
        Long deductionId = receivableService.applyDeduction(dto);

        FinReceivableDeduction deduction = deductionMapper.selectById(deductionId);
        String approvalId = "MOCK-APPR-DED-" + deductionId;
        deduction.setApprovalId(approvalId);
        deductionMapper.updateById(deduction);

        receivableService.deductionCallback(approvalId, approved);
    }

    /** 申请调整并模拟审批通过 */
    private void applyAndCallbackAdjustment(Long receivableId, int adjustType, BigDecimal amount) {
        AdjustmentCreateDTO dto = new AdjustmentCreateDTO();
        dto.setReceivableId(receivableId);
        dto.setAdjustType(adjustType);
        dto.setAdjustAmount(amount);
        dto.setReason("测试调整");
        Long adjustmentId = receivableService.applyAdjustment(dto);

        FinReceivableAdjustment adjustment = adjustmentMapper.selectById(adjustmentId);
        String approvalId = "MOCK-APPR-ADJ-" + adjustmentId;
        adjustment.setApprovalId(approvalId);
        adjustmentMapper.updateById(adjustment);

        receivableService.adjustmentCallback(approvalId, true);
    }

    // ─── RCV-01：减免不改 original_amount ───────────────────────────────────────

    @Test
    @DisplayName("RCV-01：减免审批通过后 original_amount 保持不变")
    void deductionApproved_originalAmountUnchanged() {
        BigDecimal deductAmt = new BigDecimal("200.00");

        // 申请减免
        DeductionCreateDTO dto = new DeductionCreateDTO();
        dto.setReceivableId(savedAr.getId());
        dto.setDeductionAmount(deductAmt);
        dto.setReason("测试减免");
        Long deductionId = receivableService.applyDeduction(dto);

        // 模拟OA通过
        FinReceivableDeduction deduction = deductionMapper.selectById(deductionId);
        String approvalId = "MOCK-APPR-DEDUCT-" + deductionId;
        deduction.setApprovalId(approvalId);
        deductionMapper.updateById(deduction);

        receivableService.deductionCallback(approvalId, true);

        // 验证 original_amount 不变
        FinReceivable updated = receivableMapper.selectById(savedAr.getId());
        assertThat(updated.getOriginalAmount())
                .as("original_amount 不可因减免而改变")
                .isEqualByComparingTo("1000.00");

        // 验证 deduction_amount 已累积
        assertThat(updated.getDeductionAmount())
                .as("deduction_amount 应累积200")
                .isEqualByComparingTo("200.00");

        // 验证 actual_amount = original + adjust - deduction = 1000 + 0 - 200 = 800
        assertThat(updated.getActualAmount())
                .as("actual_amount 应为800")
                .isEqualByComparingTo("800.00");

        // 验证 outstanding_amount = actual - received = 800 - 0 = 800
        assertThat(updated.getOutstandingAmount())
                .as("outstanding_amount 应为800")
                .isEqualByComparingTo("800.00");
    }

    // ─── RCV-02：actual = original + adjust - deduction 公式 ─────────────────

    @Test
    @DisplayName("RCV-02：actual = original + adjust - deduction 公式正确")
    void adjustAndDeduct_actualAmountFormula() {
        // 先做调整（增加300）
        AdjustmentCreateDTO adjDto = new AdjustmentCreateDTO();
        adjDto.setReceivableId(savedAr.getId());
        adjDto.setAdjustType(1); // 增加
        adjDto.setAdjustAmount(new BigDecimal("300.00"));
        adjDto.setReason("测试调增");
        Long adjustmentId = receivableService.applyAdjustment(adjDto);

        FinReceivableAdjustment adjustment = adjustmentMapper.selectById(adjustmentId);
        String adjApprovalId = "MOCK-APPR-ADJ-" + adjustmentId;
        adjustment.setApprovalId(adjApprovalId);
        adjustmentMapper.updateById(adjustment);
        receivableService.adjustmentCallback(adjApprovalId, true);

        // 再做减免（减少100）
        DeductionCreateDTO dedDto = new DeductionCreateDTO();
        dedDto.setReceivableId(savedAr.getId());
        dedDto.setDeductionAmount(new BigDecimal("100.00"));
        dedDto.setReason("测试减免");
        Long deductionId = receivableService.applyDeduction(dedDto);

        FinReceivableDeduction deduction = deductionMapper.selectById(deductionId);
        String dedApprovalId = "MOCK-APPR-DED-" + deductionId;
        deduction.setApprovalId(dedApprovalId);
        deductionMapper.updateById(deduction);
        receivableService.deductionCallback(dedApprovalId, true);

        // 验证：actual = 1000 + 300 - 100 = 1200
        FinReceivable updated = receivableMapper.selectById(savedAr.getId());
        assertThat(updated.getAdjustAmount())
                .as("累计调整应为300")
                .isEqualByComparingTo("300.00");
        assertThat(updated.getDeductionAmount())
                .as("累计减免应为100")
                .isEqualByComparingTo("100.00");
        assertThat(updated.getActualAmount())
                .as("actual = 1000 + 300 - 100 = 1200")
                .isEqualByComparingTo("1200.00");
        assertThat(updated.getOutstandingAmount())
                .as("outstanding = 1200 - 0 = 1200")
                .isEqualByComparingTo("1200.00");
        assertThat(updated.getOriginalAmount())
                .as("original 始终保持1000不变")
                .isEqualByComparingTo("1000.00");
    }

    // ─── RCV-03：pageQuery 按 contractId 筛选 ────────────────────────────────

    @Test
    @DisplayName("RCV-03：pageQuery 按 contractId 筛选仅返回匹配记录")
    void pageQuery_byContractId_shouldFilterCorrectly() {
        // 插入另一条不同合同的应收
        insertReceivable(CONTRACT_ID_2, MERCHANT_ID, new BigDecimal("500.00"),
                LocalDate.now().plusDays(15), 0);

        ReceivableQueryDTO query = new ReceivableQueryDTO();
        query.setContractId(CONTRACT_ID);
        query.setPageNum(1);
        query.setPageSize(50);

        IPage<ReceivableDetailVO> page = receivableService.pageQuery(query);

        assertThat(page.getRecords())
                .as("按 contractId=%d 筛选应仅返回该合同的记录", CONTRACT_ID)
                .isNotEmpty()
                .allSatisfy(vo -> assertThat(vo.getContractId()).isEqualTo(CONTRACT_ID));
    }

    // ─── RCV-04：pageQuery 按 status 筛选 ─────────────────────────────────────

    @Test
    @DisplayName("RCV-04：pageQuery 按 status=2(已收清) 筛选不返回待收记录")
    void pageQuery_byStatus_shouldFilterCorrectly() {
        ReceivableQueryDTO query = new ReceivableQueryDTO();
        query.setStatus(2); // 已收清
        query.setPageNum(1);
        query.setPageSize(50);

        IPage<ReceivableDetailVO> page = receivableService.pageQuery(query);

        assertThat(page.getRecords())
                .as("状态2筛选不应返回 setUp 中 status=0 的记录")
                .isEmpty();
    }

    // ─── RCV-05：summaryByContract 多费项汇总 ──────────────────────────────────

    @Test
    @DisplayName("RCV-05：summaryByContract 同一合同多条应收正确汇总")
    void summaryByContract_shouldAggregateMultipleFeeItems() {
        // 再插入同合同的第二条应收（500元）
        insertReceivable(CONTRACT_ID, MERCHANT_ID, new BigDecimal("500.00"),
                LocalDate.now().plusDays(60), 0);

        ReceivableQueryDTO query = new ReceivableQueryDTO();
        query.setContractId(CONTRACT_ID);

        List<ReceivableSummaryVO> summaries = receivableService.summaryByContract(query);

        assertThat(summaries)
                .as("应有且仅有1个合同汇总")
                .hasSize(1);

        ReceivableSummaryVO summary = summaries.get(0);
        assertThat(summary.getContractId()).isEqualTo(CONTRACT_ID);
        assertThat(summary.getTotalOriginal())
                .as("totalOriginal 应为 1000 + 500 = 1500")
                .isEqualByComparingTo("1500.00");
        assertThat(summary.getTotalOutstanding())
                .as("totalOutstanding 应为 1500（未收款）")
                .isEqualByComparingTo("1500.00");
    }

    // ─── RCV-06：overdueStatistics 三档分布 ────────────────────────────────────

    @Test
    @DisplayName("RCV-06：overdueStatistics 将逾期金额分到正确的三档")
    void overdueStatistics_shouldDistributeIntoThreeBuckets() {
        LocalDate today = LocalDate.now();
        // 逾期10天（≤30档）：200元
        insertReceivable(CONTRACT_ID, MERCHANT_ID, new BigDecimal("200.00"),
                today.minusDays(10), 0);
        // 逾期50天（30~90档）：300元
        insertReceivable(CONTRACT_ID, MERCHANT_ID, new BigDecimal("300.00"),
                today.minusDays(50), 0);
        // 逾期100天（>90档）：400元
        insertReceivable(CONTRACT_ID, MERCHANT_ID, new BigDecimal("400.00"),
                today.minusDays(100), 0);

        OverdueStatisticsVO stats = receivableService.overdueStatistics(PROJECT_ID);

        assertThat(stats.getOverdue30Amount())
                .as("≤30天逾期应为200")
                .isEqualByComparingTo("200.00");
        assertThat(stats.getOverdue30To90Amount())
                .as("30~90天逾期应为300")
                .isEqualByComparingTo("300.00");
        assertThat(stats.getOverdueOver90Amount())
                .as(">90天逾期应为400")
                .isEqualByComparingTo("400.00");
        assertThat(stats.getTotalOverdueAmount())
                .as("总逾期金额应为900")
                .isEqualByComparingTo("900.00");
        assertThat(stats.getTotalOverdueCount())
                .as("逾期条数应为3")
                .isEqualTo(3);
    }

    // ─── RCV-07：减免超过欠费额被拒绝 ──────────────────────────────────────────

    @Test
    @DisplayName("RCV-07：减免金额超过欠费额时应抛出异常")
    void applyDeduction_exceedingOutstanding_shouldThrow() {
        DeductionCreateDTO dto = new DeductionCreateDTO();
        dto.setReceivableId(savedAr.getId());
        dto.setDeductionAmount(new BigDecimal("1500.00")); // 超过 outstanding=1000
        dto.setReason("超额减免测试");

        assertThatThrownBy(() -> receivableService.applyDeduction(dto))
                .as("减免金额1500超过欠费额1000应抛出异常")
                .isInstanceOf(Exception.class)
                .hasMessageContaining("减免金额");
    }

    // ─── RCV-08：减免驳回不影响金额 ──────────────────────────────────────────────

    @Test
    @DisplayName("RCV-08：减免审批驳回后应收金额不变")
    void deductionRejected_shouldNotChangeAmounts() {
        applyAndCallbackDeduction(savedAr.getId(), new BigDecimal("200.00"), false);

        FinReceivable updated = receivableMapper.selectById(savedAr.getId());
        assertThat(updated.getDeductionAmount())
                .as("驳回后 deduction_amount 应仍为0")
                .isEqualByComparingTo("0.00");
        assertThat(updated.getActualAmount())
                .as("驳回后 actual_amount 应仍为1000")
                .isEqualByComparingTo("1000.00");
        assertThat(updated.getOutstandingAmount())
                .as("驳回后 outstanding_amount 应仍为1000")
                .isEqualByComparingTo("1000.00");
    }

    // ─── RCV-09：adjustType=2（调减）adjust_amount 累积为负数 ──────────────────

    @Test
    @DisplayName("RCV-09：adjustType=2 调减后 adjust_amount 为负值")
    void adjustTypeDecrease_shouldProduceNegativeAdjust() {
        applyAndCallbackAdjustment(savedAr.getId(), 2, new BigDecimal("150.00"));

        FinReceivable updated = receivableMapper.selectById(savedAr.getId());
        assertThat(updated.getAdjustAmount())
                .as("调减150，adjust_amount 应为 -150")
                .isEqualByComparingTo("-150.00");
        // actual = 1000 + (-150) - 0 = 850
        assertThat(updated.getActualAmount())
                .as("actual = 1000 - 150 = 850")
                .isEqualByComparingTo("850.00");
        assertThat(updated.getOutstandingAmount())
                .as("outstanding = 850 - 0 = 850")
                .isEqualByComparingTo("850.00");
    }

    // ─── RCV-10：markPrinted 批量标记 ──────────────────────────────────────────

    @Test
    @DisplayName("RCV-10：markPrinted 批量标记 is_printed=1")
    void markPrinted_shouldUpdateFlag() {
        FinReceivable ar2 = insertReceivable(CONTRACT_ID, MERCHANT_ID, new BigDecimal("500.00"),
                LocalDate.now().plusDays(20), 0);

        receivableService.markPrinted(Arrays.asList(savedAr.getId(), ar2.getId()));

        FinReceivable r1 = receivableMapper.selectById(savedAr.getId());
        FinReceivable r2 = receivableMapper.selectById(ar2.getId());
        assertThat(r1.getIsPrinted())
                .as("第一条应收应标记为已打印")
                .isEqualTo(1);
        assertThat(r2.getIsPrinted())
                .as("第二条应收应标记为已打印")
                .isEqualTo(1);
    }

    // ─── RCV-11：连续两次减免累积 deduction_amount ──────────────────────────────

    @Test
    @DisplayName("RCV-11：连续两次减免后 deduction_amount 累积正确")
    void consecutiveDeductions_shouldAccumulate() {
        // 第一次减免200
        applyAndCallbackDeduction(savedAr.getId(), new BigDecimal("200.00"), true);
        // 第二次减免300（此时 outstanding=800，300<800 合法）
        applyAndCallbackDeduction(savedAr.getId(), new BigDecimal("300.00"), true);

        FinReceivable updated = receivableMapper.selectById(savedAr.getId());
        assertThat(updated.getDeductionAmount())
                .as("两次减免后 deduction_amount 应为 200+300=500")
                .isEqualByComparingTo("500.00");
        // actual = 1000 + 0 - 500 = 500
        assertThat(updated.getActualAmount())
                .as("actual = 1000 - 500 = 500")
                .isEqualByComparingTo("500.00");
        assertThat(updated.getOutstandingAmount())
                .as("outstanding = 500 - 0 = 500")
                .isEqualByComparingTo("500.00");
    }

    // ─── RCV-12：全额减免 → status=3 ───────────────────────────────────────────

    @Test
    @DisplayName("RCV-12：全额减免后 status 变为3(已减免)")
    void fullDeduction_shouldSetStatusToReduced() {
        // 全额减免1000
        applyAndCallbackDeduction(savedAr.getId(), new BigDecimal("1000.00"), true);

        FinReceivable updated = receivableMapper.selectById(savedAr.getId());
        assertThat(updated.getDeductionAmount())
                .as("全额减免 deduction_amount 应为1000")
                .isEqualByComparingTo("1000.00");
        assertThat(updated.getActualAmount())
                .as("全额减免后 actual_amount 应为0")
                .isEqualByComparingTo("0.00");
        assertThat(updated.getOutstandingAmount())
                .as("全额减免后 outstanding_amount 应为0")
                .isEqualByComparingTo("0.00");
        assertThat(updated.getStatus())
                .as("全额减免后 status 应为3(已减免)")
                .isEqualTo(3);
    }
}
