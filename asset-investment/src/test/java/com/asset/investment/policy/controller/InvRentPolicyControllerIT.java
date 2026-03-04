package com.asset.investment.policy.controller;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 租决政策 Controller 集成测试（POL-I-01 ~ POL-I-04）
 * 依赖永久测试数据 inv_rent_policy(91001/91002) 和 inv_rent_policy_indicator(91001)
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
@DisplayName("租决政策 Controller 集成测试")
class InvRentPolicyControllerIT {

    private static final String BASE_URL = "/inv/rent-policies";

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // POL-I-01 分页查询政策列表
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POL-I-01 分页查询政策列表-总数>=2（含91001草稿和91002通过）")
    void polI01_pageQuery_returnsAtLeast2Records() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POL-I-02 仅返回已通过政策
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POL-I-02 /approved接口-仅返回status=2的政策-含91002不含91001")
    void polI02_approvedList_onlyApprovedStatus_contains91002() throws Exception {
        mockMvc.perform(get(BASE_URL + "/approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[*].status", everyItem(is(2))))
                .andExpect(jsonPath("$.data[*].id", hasItem(91002)))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(91001))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POL-I-03 新增政策-草稿状态
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POL-I-03 新增政策-返回Long ID-DB中status=0")
    void polI03_createPolicy_returnIdAndDraftInDB() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001);
        body.put("minLeaseTerm", 12);
        body.put("maxLeaseTerm", 36);

        String resp = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        long newId = objectMapper.readTree(resp).path("data").longValue();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_rent_policy WHERE id=?", Integer.class, newId);
        assertEquals(0, status, "新建政策 status 应为草稿(0)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POL-I-04 批量保存分类指标
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POL-I-04 批量保存91001分类指标-DB中2条指标")
    void polI04_saveIndicators_91001_2IndicatorsInDB() throws Exception {
        List<Map<String, Object>> indicators = new ArrayList<>();
        Map<String, Object> ind1 = new HashMap<>();
        ind1.put("shopCategory", 1);
        ind1.put("rentPrice", 100);
        ind1.put("propertyFeePrice", 30);
        indicators.add(ind1);

        Map<String, Object> ind2 = new HashMap<>();
        ind2.put("shopCategory", 2);
        ind2.put("rentPrice", 80);
        ind2.put("propertyFeePrice", 25);
        indicators.add(ind2);

        mockMvc.perform(post(BASE_URL + "/91001/indicators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(indicators)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inv_rent_policy_indicator WHERE policy_id=91001 AND is_deleted=0",
                Integer.class);
        assertEquals(2, count, "91001 应有2条启用指标");
    }
}
