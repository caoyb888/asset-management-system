package com.asset.finance.receivable;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.receivable.dto.AdjustmentCreateDTO;
import com.asset.finance.receivable.dto.DeductionCreateDTO;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.entity.FinReceivableAdjustment;
import com.asset.finance.receivable.entity.FinReceivableDeduction;
import com.asset.finance.receivable.mapper.FinReceivableAdjustmentMapper;
import com.asset.finance.receivable.mapper.FinReceivableDeductionMapper;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import com.asset.finance.receivable.service.FinReceivableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * 应收管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>减免审批通过后 original_amount 不变，deduction_amount 正确累积</li>
 *   <li>actual = original + adjust - deduction 公式正确</li>
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
    private static final Long MERCHANT_ID = 20002L;

    private FinReceivable savedAr;

    @BeforeEach
    void setUp() {
        // 应收：原始1000元，未收任何款项
        savedAr = new FinReceivable();
        savedAr.setReceivableCode("AR-RCV-" + System.nanoTime());
        savedAr.setContractId(CONTRACT_ID);
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

    // ─── 场景1：减免不改 original_amount ───────────────────────────────────────

    @Test
    @DisplayName("场景1：减免审批通过后 original_amount 保持不变")
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

    // ─── 场景2：actual = original + adjust - deduction 公式 ─────────────────

    @Test
    @DisplayName("场景2：actual = original + adjust - deduction 公式正确")
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
}
