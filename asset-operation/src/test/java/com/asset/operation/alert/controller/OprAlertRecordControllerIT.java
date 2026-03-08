package com.asset.operation.alert.controller;

import com.asset.operation.OperationApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 预警记录 Controller 集成测试（ALT-I）
 * 永久测试数据：
 *   预警 92001（合同到期/待发 sentStatus=0）、92002（应收到期/已发 sentStatus=1）
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
@DisplayName("预警记录 Controller 集成测试（ALT-I）")
class OprAlertRecordControllerIT {

    private static final String BASE_URL = "/opr/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // ── ALT-I-01：分页查询预警列表 ────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("ALT-I-01: 分页查询预警列表-返回code=200且total>=2")
    void pageQuery_returnsAtLeast2() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    // ── ALT-I-02：取消待发预警 ───────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("ALT-I-02: 取消待发预警92001-sentStatus变为3(已取消)")
    void cancel_updatesSentStatus() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer sentStatus = jdbc.queryForObject(
                "SELECT sent_status FROM opr_alert_record WHERE id=92001",
                Integer.class);
        assertEquals(3, sentStatus, "取消后 sent_status 应为 3(已取消)");
    }
}
