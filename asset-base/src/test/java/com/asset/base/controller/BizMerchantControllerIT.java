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
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 商家管理接口集成测试（MCH-I-01 ~ MCH-I-05）
 * 永久测试数据：
 *   商家90001（已审核通过，id_card 明文，用于脱敏测试）
 *   商家90002（待审核，用于审核接口测试）
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
@DisplayName("商家管理接口集成测试")
class BizMerchantControllerIT {

    private static final String BASE_URL = "/base/merchants";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-I-01 列表-身份证已脱敏
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-I-01 商家列表-身份证字段已脱敏含星号")
    void listMerchants_idCardMasked() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 身份证号应已脱敏为 ******** 格式（filter 返回 List，用 hasItem 检查）
                .andExpect(jsonPath("$.data.records[?(@.id==90001)].idCard",
                        org.hamcrest.Matchers.hasItem(containsString("*"))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-I-02 新增商家-身份证DB存密文
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-I-02 新增商家-含身份证-DB中存储密文而非明文")
    void createMerchant_idCardStoredAsCipher() throws Exception {
        String plainIdCard = "330104199001012345";

        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001);
        body.put("merchantName", "加密测试商家");
        body.put("merchantAttr", 2);
        body.put("idCard", plainIdCard);

        String responseStr = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long newId = objectMapper.readTree(responseStr).path("data").longValue();

        String storedIdCard = jdbc.queryForObject(
                "SELECT id_card FROM biz_merchant WHERE id=?", String.class, newId);

        // DB 中存储的应是密文，与明文不同
        assertNotEquals(plainIdCard, storedIdCard, "DB中应存储SM4密文，不应与明文相同");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-I-03 商家审核通过
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-I-03 商家审核通过-auditStatus=1-DB中更新")
    void auditMerchant_approve_updatesStatus() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("auditStatus", 1);

        mockMvc.perform(put(BASE_URL + "/{id}/audit", 90002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer auditStatus = jdbc.queryForObject(
                "SELECT audit_status FROM biz_merchant WHERE id=90002", Integer.class);
        assertEquals(1, auditStatus, "审核状态应为1（通过）");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-I-04 商家审核驳回
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-I-04 商家审核驳回-auditStatus=2-DB中更新")
    void auditMerchant_reject_updatesStatus() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("auditStatus", 2);

        mockMvc.perform(put(BASE_URL + "/{id}/audit", 90002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer auditStatus = jdbc.queryForObject(
                "SELECT audit_status FROM biz_merchant WHERE id=90002", Integer.class);
        assertEquals(2, auditStatus, "审核状态应为2（驳回）");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-I-05 分页-按审核状态过滤
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-I-05 按auditStatus=0过滤-只返回待审核记录")
    void listMerchants_filterByAuditStatus_returnsPendingOnly() throws Exception {
        mockMvc.perform(get(BASE_URL).param("auditStatus", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 所有记录的 auditStatus 应为 0
                .andExpect(jsonPath("$.data.records[*].auditStatus",
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.equalTo(0))));
    }
}
