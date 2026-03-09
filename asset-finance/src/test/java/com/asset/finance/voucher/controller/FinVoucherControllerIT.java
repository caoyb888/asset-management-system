package com.asset.finance.voucher.controller;

import com.asset.finance.FinanceControllerTestBase;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.voucher.entity.FinVoucher;
import com.asset.finance.voucher.mapper.FinVoucherMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 凭证管理 Controller 集成测试
 *
 * <p>覆盖场景：VCH-I-01 ~ VCH-I-10
 */
@DisplayName("凭证管理 Controller 集成测试")
class FinVoucherControllerIT extends FinanceControllerTestBase {

    @Autowired
    private FinVoucherMapper voucherMapper;

    @Autowired
    private FinReceiptMapper receiptMapper;

    private Map<String, Object> balancedEntries(String amount) {
        return Map.of(
                "projectId", 30003,
                "accountSet", "默认账套",
                "payType", 1,
                "voucherDate", LocalDate.now().toString(),
                "entries", List.of(
                        Map.of("accountCode", "1002", "accountName", "银行存款",
                                "debitAmount", Double.parseDouble(amount), "creditAmount", 0.00,
                                "summary", "借方"),
                        Map.of("accountCode", "1122", "accountName", "应收账款",
                                "debitAmount", 0.00, "creditAmount", Double.parseDouble(amount),
                                "summary", "贷方")
                )
        );
    }

    private Long createBalancedVoucher(String amount) throws Exception {
        String body = objectMapper.writeValueAsString(balancedEntries(amount));
        String resp = mockMvc.perform(post("/fin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").asLong();
    }

    // ─── VCH-I-01：分页列表 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-01：GET /fin/vouchers 分页查询")
    void page_shouldReturnList() throws Exception {
        createBalancedVoucher("100.00");
        createBalancedVoucher("200.00");

        mockMvc.perform(get("/fin/vouchers")
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ─── VCH-I-02：创建平衡凭证 ─────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-02：POST /fin/vouchers 创建平衡凭证")
    void create_balanced_shouldSucceed() throws Exception {
        Long voucherId = createBalancedVoucher("500.00");

        FinVoucher v = voucherMapper.selectById(voucherId);
        assertThat(v.getStatus()).as("新建凭证 status 应为0").isEqualTo(0);
    }

    // ─── VCH-I-03：不平衡凭证 ───────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-03：POST /fin/vouchers 借贷不平衡应失败")
    void create_unbalanced_shouldFail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "projectId", 30003,
                "accountSet", "默认账套",
                "payType", 1,
                "voucherDate", LocalDate.now().toString(),
                "entries", List.of(
                        Map.of("accountCode", "1002", "accountName", "银行存款",
                                "debitAmount", 1000.00, "creditAmount", 0.00, "summary", "借方"),
                        Map.of("accountCode", "1122", "accountName", "应收账款",
                                "debitAmount", 0.00, "creditAmount", 800.00, "summary", "贷方")
                )
        ));

        mockMvc.perform(post("/fin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    // ─── VCH-I-04：凭证详情 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-04：GET /fin/vouchers/{id} 返回含分录详情")
    void getDetail_shouldContainEntries() throws Exception {
        Long voucherId = createBalancedVoucher("300.00");

        mockMvc.perform(get("/fin/vouchers/{id}", voucherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.entries").isArray())
                .andExpect(jsonPath("$.data.entries.length()").value(2));
    }

    // ─── VCH-I-05：审核凭证 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-05：POST /fin/vouchers/{id}/audit 审核凭证")
    void audit_shouldSetStatus1() throws Exception {
        Long voucherId = createBalancedVoucher("400.00");

        mockMvc.perform(post("/fin/vouchers/{id}/audit", voucherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        FinVoucher v = voucherMapper.selectById(voucherId);
        assertThat(v.getStatus()).as("审核后 status 应为1").isEqualTo(1);
    }

    // ─── VCH-I-06：上传凭证 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-06：POST /fin/vouchers/{id}/upload 上传凭证")
    void upload_shouldSetStatus2() throws Exception {
        Long voucherId = createBalancedVoucher("500.00");
        mockMvc.perform(post("/fin/vouchers/{id}/audit", voucherId));

        mockMvc.perform(post("/fin/vouchers/{id}/upload", voucherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        FinVoucher v = voucherMapper.selectById(voucherId);
        assertThat(v.getStatus()).as("上传后 status 应为2").isEqualTo(2);
        assertThat(v.getUploadTime()).as("upload_time 应非空").isNotNull();
    }

    // ─── VCH-I-07：重复上传 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-07：POST /fin/vouchers/{id}/upload 重复上传应失败")
    void upload_duplicate_shouldFail() throws Exception {
        Long voucherId = createBalancedVoucher("600.00");
        mockMvc.perform(post("/fin/vouchers/{id}/audit", voucherId));
        mockMvc.perform(post("/fin/vouchers/{id}/upload", voucherId));

        mockMvc.perform(post("/fin/vouchers/{id}/upload", voucherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    // ─── VCH-I-08：删除草稿 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-08：DELETE /fin/vouchers/{id} 删除草稿凭证")
    void delete_draft_shouldSucceed() throws Exception {
        Long voucherId = createBalancedVoucher("700.00");

        mockMvc.perform(delete("/fin/vouchers/{id}", voucherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        FinVoucher v = voucherMapper.selectById(voucherId);
        assertThat(v).as("逻辑删除后 selectById 应返回null").isNull();
    }

    // ─── VCH-I-09：已审核不可删 ─────────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-09：DELETE /fin/vouchers/{id} 已审核凭证不可删除")
    void delete_audited_shouldFail() throws Exception {
        Long voucherId = createBalancedVoucher("800.00");
        mockMvc.perform(post("/fin/vouchers/{id}/audit", voucherId));

        mockMvc.perform(delete("/fin/vouchers/{id}", voucherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    // ─── VCH-I-10：从收款单生成凭证 ─────────────────────────────────────────────

    @Test
    @DisplayName("VCH-I-10：POST /fin/vouchers/generate-from-receipt/{id} 生成凭证")
    void generateFromReceipt_shouldCreateVoucher() throws Exception {
        FinReceipt receipt = new FinReceipt();
        receipt.setReceiptCode("RC-VIT-" + System.nanoTime());
        receipt.setContractId(10001L);
        receipt.setProjectId(30003L);
        receipt.setTotalAmount(new BigDecimal("2000.00"));
        receipt.setPaymentMethod(1);
        receipt.setReceiptDate(LocalDate.now());
        receipt.setStatus(2);
        receipt.setWriteOffAmount(new BigDecimal("2000.00"));
        receipt.setPrepayAmount(BigDecimal.ZERO);
        receiptMapper.insert(receipt);

        String response = mockMvc.perform(post("/fin/vouchers/generate-from-receipt/{id}", receipt.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long voucherId = objectMapper.readTree(response).get("data").asLong();
        FinVoucher voucher = voucherMapper.selectById(voucherId);
        assertThat(voucher.getTotalDebit())
                .as("借方合计应等于贷方合计")
                .isEqualByComparingTo(voucher.getTotalCredit());
    }
}
