package com.asset.finance.voucher;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.voucher.dto.*;
import com.asset.finance.voucher.entity.FinVoucher;
import com.asset.finance.voucher.entity.FinVoucherEntry;
import com.asset.finance.voucher.mapper.FinVoucherEntryMapper;
import com.asset.finance.voucher.mapper.FinVoucherMapper;
import com.asset.finance.voucher.service.FinVoucherService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
 *   <li>VCH-01：借贷不平衡提交审核 → 抛出 FIN_4006</li>
 *   <li>VCH-02：凭证已上传后重复上传 → 抛出 FIN_4007</li>
 *   <li>VCH-03：正常审核流程：status 0→1→2</li>
 *   <li>VCH-04：未审核直接上传 → 业务异常</li>
 *   <li>VCH-05：从收款单生成凭证-标准2笔分录</li>
 *   <li>VCH-06：凭证分页查询-按状态筛选</li>
 *   <li>VCH-07：凭证详情-含分录列表</li>
 *   <li>VCH-08：删除凭证-仅草稿可删</li>
 *   <li>VCH-09：删除凭证-已审核不可删</li>
 *   <li>VCH-10：上传后 upload_time 自动记录</li>
 * </ol>
 */
@DisplayName("凭证管理 Service 测试")
class FinVoucherServiceTest extends FinanceTestBase {

    @Autowired
    private FinVoucherService voucherService;

    @Autowired
    private FinVoucherMapper voucherMapper;

    @Autowired
    private FinVoucherEntryMapper entryMapper;

    @Autowired
    private FinReceiptMapper receiptMapper;

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

    // ─── VCH-05：从收款单生成凭证 ──────────────────────────────────────────────

    @Test
    @DisplayName("VCH-05：从收款单自动生成凭证-含标准2笔分录，借贷平衡")
    void generateFromReceipt_shouldCreateBalancedVoucher() {
        // 插入已核销收款单
        FinReceipt receipt = new FinReceipt();
        receipt.setReceiptCode("RC-VCH-" + System.nanoTime());
        receipt.setContractId(10001L);
        receipt.setProjectId(30003L);
        receipt.setTotalAmount(new BigDecimal("2000.00"));
        receipt.setPaymentMethod(1);
        receipt.setReceiptDate(LocalDate.now());
        receipt.setStatus(2); // 已核销
        receipt.setWriteOffAmount(new BigDecimal("2000.00"));
        receipt.setPrepayAmount(BigDecimal.ZERO);
        receiptMapper.insert(receipt);

        Long voucherId = voucherService.generateFromReceipt(receipt.getId());

        assertThat(voucherId).as("应返回非空凭证ID").isNotNull();

        FinVoucher voucher = voucherMapper.selectById(voucherId);
        assertThat(voucher.getVoucherCode()).as("凭证号应以VC-开头").startsWith("VC-");
        assertThat(voucher.getStatus()).as("状态应为0(待审核)").isEqualTo(0);
        assertThat(voucher.getTotalDebit())
                .as("借方合计应为2000")
                .isEqualByComparingTo("2000.00");
        assertThat(voucher.getTotalCredit())
                .as("贷方合计应为2000")
                .isEqualByComparingTo("2000.00");

        // 验证分录
        List<FinVoucherEntry> entries = entryMapper.selectByVoucherId(voucherId);
        assertThat(entries).as("应有2条分录").hasSize(2);

        // 借方：银行存款
        FinVoucherEntry debitEntry = entries.stream()
                .filter(e -> e.getDebitAmount().compareTo(BigDecimal.ZERO) > 0)
                .findFirst().orElse(null);
        assertThat(debitEntry).as("应有借方分录").isNotNull();
        assertThat(debitEntry.getAccountCode()).isEqualTo("1002");

        // 贷方：应收账款
        FinVoucherEntry creditEntry = entries.stream()
                .filter(e -> e.getCreditAmount().compareTo(BigDecimal.ZERO) > 0)
                .findFirst().orElse(null);
        assertThat(creditEntry).as("应有贷方分录").isNotNull();
        assertThat(creditEntry.getAccountCode()).isEqualTo("1122");
    }

    // ─── VCH-06：凭证分页查询-按状态筛选 ──────────────────────────────────────────

    @Test
    @DisplayName("VCH-06：pageQuery 按 status=1 仅返回已审核凭证")
    void pageQuery_byStatus_shouldFilterCorrectly() {
        // 创建3张凭证：status=0, 0→1, 0→1→2
        Long v0 = createBalancedVoucher("100.00");
        Long v1 = createBalancedVoucher("200.00");
        voucherService.audit(v1);
        Long v2 = createBalancedVoucher("300.00");
        voucherService.audit(v2);
        voucherService.upload(v2);

        VoucherQueryDTO query = new VoucherQueryDTO();
        query.setStatus(1); // 已审核
        query.setPageNum(1);
        query.setPageSize(50);

        IPage<VoucherDetailVO> page = voucherService.pageQuery(query);

        assertThat(page.getRecords())
                .as("按status=1筛选应仅返回已审核凭证")
                .isNotEmpty()
                .allSatisfy(vo -> assertThat(vo.getStatus()).isEqualTo(1));
    }

    // ─── VCH-07：凭证详情-含分录列表 ──────────────────────────────────────────────

    @Test
    @DisplayName("VCH-07：getDetail 返回含分录列表的凭证详情")
    void getDetail_shouldContainEntries() {
        Long voucherId = createBalancedVoucher("500.00");

        VoucherDetailVO vo = voucherService.getDetail(voucherId);

        assertThat(vo).as("详情VO不应为空").isNotNull();
        assertThat(vo.getEntries())
                .as("详情应包含2条分录")
                .hasSize(2);
        assertThat(vo.getTotalDebit())
                .as("借方合计应等于贷方合计")
                .isEqualByComparingTo(vo.getTotalCredit());
    }

    // ─── VCH-08：删除凭证-仅草稿可删 ──────────────────────────────────────────────

    @Test
    @DisplayName("VCH-08：删除 status=0 草稿凭证成功")
    void deleteVoucher_draft_shouldSucceed() {
        Long voucherId = createBalancedVoucher("400.00");

        voucherService.deleteVoucher(voucherId);

        FinVoucher deleted = voucherMapper.selectById(voucherId);
        assertThat(deleted)
                .as("逻辑删除后 selectById 应返回null（is_deleted=1被过滤）")
                .isNull();

        // 分录也应被删除
        List<FinVoucherEntry> entries = entryMapper.selectByVoucherId(voucherId);
        assertThat(entries)
                .as("分录也应被逻辑删除")
                .isEmpty();
    }

    // ─── VCH-09：删除凭证-已审核不可删 ────────────────────────────────────────────

    @Test
    @DisplayName("VCH-09：删除已审核(status=1)凭证应抛出异常")
    void deleteVoucher_audited_shouldThrow() {
        Long voucherId = createBalancedVoucher("600.00");
        voucherService.audit(voucherId);

        assertThatThrownBy(() -> voucherService.deleteVoucher(voucherId))
                .as("已审核凭证不可删除，应抛出异常")
                .isInstanceOf(Exception.class);
    }

    // ─── VCH-10：上传后 upload_time 自动记录 ──────────────────────────────────────

    @Test
    @DisplayName("VCH-10：上传后 upload_time 非空")
    void upload_shouldSetUploadTime() {
        Long voucherId = createBalancedVoucher("700.00");
        voucherService.audit(voucherId);
        voucherService.upload(voucherId);

        FinVoucher uploaded = voucherMapper.selectById(voucherId);
        assertThat(uploaded.getUploadTime())
                .as("上传后 upload_time 应非空")
                .isNotNull();
        assertThat(uploaded.getStatus())
                .as("上传后 status 应为2")
                .isEqualTo(2);
    }

    // ─── 私有构建方法 ──────────────────────────────────────────────────────────

    /** 创建一张借贷平衡的凭证，返回ID */
    private Long createBalancedVoucher(String amount) {
        VoucherCreateDTO dto = buildVoucherDTO(
                entry("1002", "银行存款", amount, "0.00"),
                entry("1122", "应收账款", "0.00", amount)
        );
        return voucherService.createVoucher(dto);
    }

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
