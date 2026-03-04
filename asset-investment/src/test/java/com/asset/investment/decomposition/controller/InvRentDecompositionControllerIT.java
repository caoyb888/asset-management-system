package com.asset.investment.decomposition.controller;

import com.asset.investment.InvestmentApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 租金分解 Controller 集成测试（DECOMP-I-01 ~ DECOMP-I-04）
 * 依赖永久测试数据 inv_rent_decomposition(91001/91002) 和 inv_rent_decomp_detail(91001)
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = InvestmentApplication.class,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false"
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("租金分解 Controller 集成测试")
class InvRentDecompositionControllerIT {

    private static final String BASE_URL = "/inv/rent-decomps";

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-I-01 分页查询分解列表
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-I-01 分页查询分解列表-总数>=2（含91001草稿和91002审批中）")
    void decompI01_pageQuery_returnsAtLeast2Records() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-I-02 批量保存明细-自动计算年租金
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-I-02 批量保存91001明细-shopCategory=1,price=100,area=500-annual_rent=600000")
    void decompI02_saveDetails_autoCalcAnnualRent() throws Exception {
        List<Map<String, Object>> details = new ArrayList<>();
        Map<String, Object> detail = new HashMap<>();
        detail.put("shopCategory", 1);
        detail.put("rentUnitPrice", 100);
        detail.put("propertyUnitPrice", 30);
        detail.put("area", 500);
        details.add(detail);

        mockMvc.perform(post(BASE_URL + "/91001/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 100 × 500 × 12 = 600,000
        BigDecimal annualRent = jdbc.queryForObject(
                "SELECT annual_rent FROM inv_rent_decomp_detail WHERE decomp_id=91001 AND shop_category=1 AND is_deleted=0",
                BigDecimal.class);
        assertEquals(0, new BigDecimal("600000.00").compareTo(annualRent),
                "annual_rent 应为 600000.00");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-I-03 汇总计算-totalAnnualRent 更新
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-I-03 汇总计算-91001含明细(annualRent=600000)-total_annual_rent=600000")
    void decompI03_calculate_91001_totalAnnualRentUpdated() throws Exception {
        // 先保存一条明细使 91001 有 annualRent 数据
        List<Map<String, Object>> details = new ArrayList<>();
        Map<String, Object> detail = new HashMap<>();
        detail.put("shopCategory", 1);
        detail.put("rentUnitPrice", 100);
        detail.put("propertyUnitPrice", 0);
        detail.put("area", 500);
        details.add(detail);
        mockMvc.perform(post(BASE_URL + "/91001/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(details)));

        // 触发汇总计算
        mockMvc.perform(post(BASE_URL + "/91001/calculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BigDecimal totalRent = jdbc.queryForObject(
                "SELECT total_annual_rent FROM inv_rent_decomposition WHERE id=91001",
                BigDecimal.class);
        assertEquals(0, new BigDecimal("600000.00").compareTo(totalRent),
                "total_annual_rent 应为 600000.00");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-I-04 发起审批-草稿→审批中
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-I-04 发起审批-91001草稿→DB中status=1")
    void decompI04_submitApproval_91001_statusBecomesApproving() throws Exception {
        mockMvc.perform(post(BASE_URL + "/91001/submit-approval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_rent_decomposition WHERE id=91001", Integer.class);
        assertEquals(1, status, "91001 status 应变为审批中(1)");
    }
}
