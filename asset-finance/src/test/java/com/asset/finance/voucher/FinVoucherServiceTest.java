package com.asset.finance.voucher;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.voucher.dto.VoucherCreateDTO;
import com.asset.finance.voucher.dto.VoucherEntryDTO;
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
 * 凭证管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>借贷不平衡提交审核 → 抛出 FIN_4006</li>
 *   <li>凭证已上传后重复上传 → 抛出 FIN_4007，upload_status 只变一次</li>
 *   <li>正常审核流程：status 0→1→2</li>
 * </ol>
 */
@DisplayName("凭证管理 Service 测试")
class FinVoucherServiceTest extends FinanceTestBase {

    @Autowired
    private FinVoucherService voucherService;

    @Autowired
    private FinVoucherMapper voucherMapper;

    // ─── 场景1：借贷不平衡 → FIN_4006 ────────────────────────────────────────

    @Test
    @DisplayName("场景1：借贷不平衡提交 → 抛出 FIN_4006")
    void createVoucher_unbalancedDebitCredit_shouldThrowFIN4006() {
        VoucherCreateDTO dto = buildVoucherDTO(
                entry("1002", "银行存款", "1000.00", "0.00"),
                entry("1122", "应收账款", "0.00",   "800.00")  // 借1000 贷800，差200
        );

        assertThatThrownBy(() -> voucherService.createVoucher(dto))
                .isInstanceOf(FinBizException.class)
                .extracting(e -> ((FinBizException) e).getCode())
                .isEqualTo(FinErrorCode.FIN_4006.getCode());
    }

    // ─── 场景2：重复上传幂等 → FIN_4007 ─────────────────────────────────────

    @Test
    @DisplayName("场景2：凭证重复上传 → 第2次抛 FIN_4007，upload_status 只变一次")
    void upload_duplicate_shouldThrowFIN4007_uploadStatusChangedOnce() {
        // 创建平衡凭证
        VoucherCreateDTO dto = buildVoucherDTO(
                entry("1002", "银行存款",  "500.00", "0.00"),
                entry("1122", "应收账款",  "0.00",   "500.00")
        );
        Long voucherId = voucherService.createVoucher(dto);

        // 审核（0→1）
        voucherService.audit(voucherId);

        // 第一次上传（1→2）
        voucherService.upload(voucherId);
        FinVoucher afterFirstUpload = voucherMapper.selectById(voucherId);
        assertThat(afterFirstUpload.getStatus())
                .as("第一次上传后 status 应为2")
                .isEqualTo(2);

        // 第二次重复上传 → FIN_4007
        assertThatThrownBy(() -> voucherService.upload(voucherId))
                .isInstanceOf(FinBizException.class)
                .extracting(e -> ((FinBizException) e).getCode())
                .isEqualTo(FinErrorCode.FIN_4007.getCode());

        // 确认 DB 中 status 仍为2（未多次触发）
        FinVoucher afterSecondAttempt = voucherMapper.selectById(voucherId);
        assertThat(afterSecondAttempt.getStatus())
                .as("重复上传后 status 不应改变，仍为2")
                .isEqualTo(2);
    }

    // ─── 场景3：正常审核流程 status 0 → 1 → 2 ─────────────────────────────────

    @Test
    @DisplayName("场景3：凭证正常流转 status 0→1→2")
    void normalAuditAndUpload_statusTransitions() {
        VoucherCreateDTO dto = buildVoucherDTO(
                entry("1002", "银行存款",  "1000.00", "0.00"),
                entry("1122", "应收账款",  "0.00",    "1000.00")
        );
        Long voucherId = voucherService.createVoucher(dto);

        FinVoucher created = voucherMapper.selectById(voucherId);
        assertThat(created.getStatus()).as("创建后 status 应为0（待审核）").isEqualTo(0);

        // 审核
        voucherService.audit(voucherId);
        FinVoucher audited = voucherMapper.selectById(voucherId);
        assertThat(audited.getStatus()).as("审核后 status 应为1").isEqualTo(1);

        // 上传
        voucherService.upload(voucherId);
        FinVoucher uploaded = voucherMapper.selectById(voucherId);
        assertThat(uploaded.getStatus()).as("上传后 status 应为2").isEqualTo(2);
        assertThat(uploaded.getUploadTime()).as("upload_time 应已设置").isNotNull();
    }

    // ─── 场景4：未审核直接上传 → 业务异常 ────────────────────────────────────

    @Test
    @DisplayName("场景4：未审核（status=0）直接上传 → 抛出业务异常")
    void upload_beforeAudit_shouldThrowBizException() {
        VoucherCreateDTO dto = buildVoucherDTO(
                entry("1002", "银行存款",  "300.00", "0.00"),
                entry("1122", "应收账款",  "0.00",   "300.00")
        );
        Long voucherId = voucherService.createVoucher(dto);

        // 直接上传（跳过审核）
        assertThatThrownBy(() -> voucherService.upload(voucherId))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("未审核");
    }

    // ─── 私有构建方法 ──────────────────────────────────────────────────────────

    private VoucherCreateDTO buildVoucherDTO(VoucherEntryDTO... entries) {
        VoucherCreateDTO dto = new VoucherCreateDTO();
        dto.setProjectId(30003L);
        dto.setAccountSet("默认账套");
        dto.setPayType(1);
        dto.setVoucherDate(LocalDate.now());
        dto.setRemark("测试凭证");
        dto.setEntries(List.of(entries));
        return dto;
    }

    private VoucherEntryDTO entry(String code, String name, String debit, String credit) {
        VoucherEntryDTO e = new VoucherEntryDTO();
        e.setAccountCode(code);
        e.setAccountName(name);
        e.setDebitAmount(new BigDecimal(debit));
        e.setCreditAmount(new BigDecimal(credit));
        e.setSummary("测试分录-" + name);
        return e;
    }
}
