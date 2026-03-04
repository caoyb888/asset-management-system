package com.asset.base.controller;

import com.asset.base.BaseApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 品牌管理接口集成测试（BRAND-I-01 ~ BRAND-I-04）
 * 永久测试数据：
 *   品牌90001（星巴克测试品牌），含2个联系人（id=90001/90002）
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = BaseApplication.class,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false"
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("品牌管理接口集成测试")
class BizBrandControllerIT {

    private static final String BASE_URL = "/base/brands";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-I-01 分页查询品牌
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-I-01 分页查询品牌-返回code=200且total>=1")
    void listBrands_returnsAtLeastOne() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-I-02 详情-含联系人
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-I-02 品牌详情-id=90001-contacts非空")
    void getBrand_includesContacts() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 90001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.brandNameCn").value(org.hamcrest.Matchers.containsString("星巴克")))
                .andExpect(jsonPath("$.data.contacts").isArray())
                .andExpect(jsonPath("$.data.contacts", not(empty())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-I-03 新增品牌
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-I-03 新增品牌-合法数据-返回200")
    void createBrand_validData_returnsId() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("brandNameCn", "集成测试品牌");
        body.put("brandLevel", 2);
        body.put("cooperationType", 1);
        body.put("businessNature", 1);
        body.put("chainType", 1);
        body.put("brandType", 1);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-I-04 编辑品牌-联系人重建
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-I-04 编辑品牌90001-重建联系人-旧联系人消失，新联系人存在")
    void updateBrand_rebuildsContacts() throws Exception {
        // 构造新联系人（只有1个，替换原有2个）
        Map<String, Object> newContact = new HashMap<>();
        newContact.put("contactName", "新联系人-IT测试");
        newContact.put("phone", "13900000099");
        newContact.put("isPrimary", 1);

        Map<String, Object> body = new HashMap<>();
        body.put("brandNameCn", "星巴克（编辑测试）");
        body.put("contacts", List.of(newContact));

        mockMvc.perform(put(BASE_URL + "/{id}", 90001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证只剩1个联系人（重建后只有新联系人）
        Integer contactCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_brand_contact WHERE brand_id=90001 AND is_deleted=0",
                Integer.class);
        assertEquals(1, contactCount, "重建后应只剩1个联系人");

        // 验证新联系人存在
        Integer newContactCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_brand_contact WHERE brand_id=90001 AND contact_name='新联系人-IT测试' AND is_deleted=0",
                Integer.class);
        assertEquals(1, newContactCount, "新联系人应已创建");
    }
}
