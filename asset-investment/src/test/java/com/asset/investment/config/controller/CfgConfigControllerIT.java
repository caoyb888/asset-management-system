package com.asset.investment.config.controller;

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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 配置管理 Controller 集成测试（CFG-I-01 ~ CFG-I-05）
 * 依赖永久测试数据 cfg_rent_scheme(91001/91002) 和 cfg_fee_item(91001/91002/91003)
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
@DisplayName("配置管理 Controller 集成测试")
class CfgConfigControllerIT {

    private static final String BASE_URL_SCHEME = "/inv/config/rent-schemes";
    private static final String BASE_URL_FEE    = "/inv/config/fee-items";

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // CFG-I-01 查询计租方案列表（仅启用）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CFG-I-01 查询计租方案列表-默认仅启用-含91001不含91002")
    void cfgI01_listRentSchemes_onlyEnabled_contains91001NotContain91002() throws Exception {
        mockMvc.perform(get(BASE_URL_SCHEME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[*].id", hasItem(91001)))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(91002))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CFG-I-02 新增计租方案
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CFG-I-02 新增计租方案-返回新ID-DB中记录存在")
    void cfgI02_createRentScheme_returnsIdAndPersists() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("schemeCode", "TEST-SCH-001");
        body.put("schemeName", "测试固定方案");
        body.put("chargeType", 1);
        body.put("status", 1);

        String resp = mockMvc.perform(post(BASE_URL_SCHEME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        long newId = objectMapper.readTree(resp).path("data").longValue();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cfg_rent_scheme WHERE id=? AND is_deleted=0", Integer.class, newId);
        assertEquals(1, count, "新增的计租方案应在DB中存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CFG-I-03 启用停用方案切换
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CFG-I-03 启用/停用切换-91002由停用变为启用-DB status=1")
    void cfgI03_toggleStatus_91002StoppedToEnabled() throws Exception {
        Map<String, Integer> body = new HashMap<>();
        body.put("status", 1);

        mockMvc.perform(put(BASE_URL_SCHEME + "/91002/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM cfg_rent_scheme WHERE id=91002", Integer.class);
        assertEquals(1, status, "91002 的 status 应已被更新为 1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CFG-I-04 查询费项-默认只返回启用-不含91003停用项
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CFG-I-04 查询费项列表-默认仅启用-不含91003停用项")
    void cfgI04_listFeeItems_onlyEnabled_notContains91003() throws Exception {
        mockMvc.perform(get(BASE_URL_FEE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(91003))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CFG-I-05 新增费项-租金类自动必填
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CFG-I-05 新增费项-租金类itemType=1-DB中is_required自动置1")
    void cfgI05_createFeeItem_rentType_isRequiredAutoSet() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("itemCode", "FEE-TEST-01");
        body.put("itemName", "测试租金");
        body.put("itemType", 1);
        body.put("status", 1);

        String resp = mockMvc.perform(post(BASE_URL_FEE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        long newId = objectMapper.readTree(resp).path("data").longValue();
        Integer isRequired = jdbc.queryForObject(
                "SELECT is_required FROM cfg_fee_item WHERE id=?", Integer.class, newId);
        assertEquals(1, isRequired, "租金类费项 is_required 应自动置为 1");
    }
}
