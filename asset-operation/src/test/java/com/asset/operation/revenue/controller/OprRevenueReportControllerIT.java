package com.asset.operation.revenue.controller;

import com.asset.operation.OperationApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 营收填报 Controller 集成测试（REV-I）
 * 永久测试数据：
 *   营收 92001（待确认/2026-01-15/50000）、92002（已确认/2026-01-16/60000）
 *   浮动租金 92001（合同91003/2025-12/浮动租金5000）
 *   合同 91003、商铺 90001、项目 90001、商家 90002
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OperationApplication.class,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false"
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("营收填报 Controller 集成测试（REV-I）")
class OprRevenueReportControllerIT {

    private static final String BASE_URL = "/opr/revenue-reports";
    private static final String FLOATING_URL = "/opr/floating-rents";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── REV-I-01：分页查询营收列表 ────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("REV-I-01: 分页查询营收列表-返回code=200且total>=2")
    void pageQuery_returnsAtLeast2() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    // ── REV-I-02：新增营收填报 ───────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("REV-I-02: 新增营收填报-返回新记录ID且status=0")
    void create_returnsNewId() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contractId", 91003L);
        body.put("reportDate", "2026-02-01");
        body.put("revenueAmount", 45000);

        String response = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value(0))
                .andReturn().getResponse().getContentAsString();

        Long newId = objectMapper.readTree(response).path("data").path("id").asLong();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_revenue_report WHERE id=? AND is_deleted=0",
                Integer.class, newId);
        assertEquals(0, status, "新营收记录 status 应为 0(待确认)");
    }

    // ── REV-I-03：编辑待确认营收 ─────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("REV-I-03: 编辑待确认营收92001-金额更新为55000")
    void updatePending_updatesAmount() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contractId", 91003L);
        body.put("reportDate", "2026-01-15");
        body.put("revenueAmount", 55000);

        mockMvc.perform(put(BASE_URL + "/{id}", 92001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BigDecimal amount = jdbc.queryForObject(
                "SELECT revenue_amount FROM opr_revenue_report WHERE id=92001",
                BigDecimal.class);
        assertEquals(0, new BigDecimal("55000").compareTo(amount),
                "营业额应更新为 55000");
    }

    // ── REV-I-04：编辑已确认-拒绝 ───────────────────────────────

    @Test
    @Order(4)
    @DisplayName("REV-I-04: 编辑已确认营收92002-返回code≠200")
    void updateConfirmed_rejected() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contractId", 91003L);
        body.put("reportDate", "2026-01-16");
        body.put("revenueAmount", 70000);

        mockMvc.perform(put(BASE_URL + "/{id}", 92002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ── REV-I-05：日历视图数据 ───────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("REV-I-05: 日历视图数据-contractId=91003&month=2026-01")
    void dailyDetail_returnsData() throws Exception {
        mockMvc.perform(get(BASE_URL + "/daily-detail")
                        .param("contractId", "91003")
                        .param("reportMonth", "2026-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    // ── REV-I-06：月度汇总统计 ───────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("REV-I-06: 月度汇总统计-reportMonth=2026-01")
    void statistics_returnsData() throws Exception {
        mockMvc.perform(get(BASE_URL + "/statistics")
                        .param("reportMonth", "2026-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    // ── REV-I-07：浮动租金列表 ───────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("REV-I-07: 浮动租金列表-返回total>=1")
    void floatingRentList_returnsAtLeast1() throws Exception {
        mockMvc.perform(get(FLOATING_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    // ── REV-I-08：浮动租金详情 ───────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("REV-I-08: 浮动租金详情92001-floatingRent=5000")
    void floatingRentDetail_returnsData() throws Exception {
        mockMvc.perform(get(FLOATING_URL + "/{id}", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.floatingRent").value(5000));
    }
}
