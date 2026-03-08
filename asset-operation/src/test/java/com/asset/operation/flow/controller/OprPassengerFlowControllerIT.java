package com.asset.operation.flow.controller;

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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 客流填报 Controller 集成测试（PF-I）
 * 永久测试数据：
 *   客流 92001（手动/2026-01-15/1500人）、92002（导入/2026-01-16/1800人）
 *   项目 90001、楼栋 90001、楼层 90001
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
@DisplayName("客流填报 Controller 集成测试（PF-I）")
class OprPassengerFlowControllerIT {

    private static final String BASE_URL = "/opr/passenger-flows";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── PF-I-01：分页查询客流列表 ────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("PF-I-01: 分页查询客流列表-返回code=200且total>=2")
    void pageQuery_returnsAtLeast2() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    // ── PF-I-02：新增客流填报 ────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("PF-I-02: 新增客流填报-返回ID且flowCount=2000")
    void create_returnsNewId() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001L);
        body.put("buildingId", 90001L);
        body.put("reportDate", "2026-02-01");
        body.put("flowCount", 2000);

        String response = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long newId = objectMapper.readTree(response).path("data").asLong();
        Integer flowCount = jdbc.queryForObject(
                "SELECT flow_count FROM opr_passenger_flow WHERE id=? AND is_deleted=0",
                Integer.class, newId);
        assertEquals(2000, flowCount, "新客流记录 flow_count 应为 2000");
    }

    // ── PF-I-03：编辑手动录入客流 ────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("PF-I-03: 编辑手动录入客流92001-flowCount更新为1600")
    void updateManual_updatesFlowCount() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001L);
        body.put("buildingId", 90001L);
        body.put("reportDate", "2026-01-15");
        body.put("flowCount", 1600);

        mockMvc.perform(put(BASE_URL + "/{id}", 92001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer flowCount = jdbc.queryForObject(
                "SELECT flow_count FROM opr_passenger_flow WHERE id=92001",
                Integer.class);
        assertEquals(1600, flowCount, "客流应更新为 1600");
    }

    // ── PF-I-04：统计分析 ────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("PF-I-04: 统计分析projectId=90001-返回统计数据")
    void statistics_returnsData() throws Exception {
        mockMvc.perform(get(BASE_URL + "/statistics")
                        .param("projectId", "90001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }
}
