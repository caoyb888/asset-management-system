package com.asset.investment.opening.controller;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 开业审批 Controller 集成测试（OA-I-01 ~ OA-I-05）
 * 依赖永久测试数据 inv_opening_approval(91001/91002/91003)
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
@DisplayName("开业审批 Controller 集成测试")
class InvOpeningApprovalControllerIT {

    private static final String BASE_URL = "/inv/opening-approvals";

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // OA-I-01 分页查询审批列表
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("OA-I-01 分页查询开业审批列表-总数>=3（含91001~91003）")
    void oaI01_pageQuery_returnsAtLeast3Records() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(3)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OA-I-02 新增开业审批-草稿
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("OA-I-02 新增开业审批-返回Long ID-DB中status=0")
    void oaI02_createApproval_returnIdAndDraftInDB() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001);
        body.put("contractId", 91003);
        body.put("plannedOpeningDate", "2026-08-01");

        String resp = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        long newId = objectMapper.readTree(resp).path("data").longValue();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_opening_approval WHERE id=?", Integer.class, newId);
        assertEquals(0, status, "新建开业审批 status 应为草稿(0)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OA-I-03 提交审批-草稿→审批中
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("OA-I-03 提交审批-91001草稿→DB中status=1")
    void oaI03_submitApproval_91001_statusBecomesApproving() throws Exception {
        mockMvc.perform(post(BASE_URL + "/91001/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_opening_approval WHERE id=91001", Integer.class);
        assertEquals(1, status, "91001 status 应变为审批中(1)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OA-I-04 审批驳回-状态变3且保存快照
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("OA-I-04 审批驳回回调-91002(审批中)→status=3且snapshot_data非NULL")
    void oaI04_approvalCallbackRejected_91002_statusRejectedAndSnapshotSaved() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approved", false);

        mockMvc.perform(post(BASE_URL + "/91002/approval-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_opening_approval WHERE id=91002", Integer.class);
        assertEquals(3, status, "91002 status 应变为驳回(3)");

        Object snapshot = jdbc.queryForObject(
                "SELECT snapshot_data FROM inv_opening_approval WHERE id=91002", Object.class);
        assertNotNull(snapshot, "驳回时应保存快照数据");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OA-I-05 基于驳回单创建新单
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("OA-I-05 基于驳回单91003创建新单-新单status=0且previous_approval_id=91003")
    void oaI05_createFromPrevious_91003Rejected_newApprovalDraftWithPreviousId() throws Exception {
        String resp = mockMvc.perform(post(BASE_URL + "/from-previous/91003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        long newId = objectMapper.readTree(resp).path("data").longValue();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_opening_approval WHERE id=?", Integer.class, newId);
        Long previousId = jdbc.queryForObject(
                "SELECT previous_approval_id FROM inv_opening_approval WHERE id=?", Long.class, newId);

        assertEquals(0, status, "新单 status 应为草稿(0)");
        assertEquals(91003L, previousId, "新单 previous_approval_id 应为 91003");
    }
}
