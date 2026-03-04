package com.asset.investment.intention.controller;

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
 * 意向协议 Controller 集成测试（INT-I-01 ~ INT-I-07）
 * 依赖永久测试数据 inv_intention(91001~91004)
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
@DisplayName("意向协议 Controller 集成测试")
class InvIntentionControllerIT {

    private static final String BASE_URL = "/inv/intentions";

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // INT-I-01 分页查询意向列表
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-I-01 分页查询意向列表-总数>=4（含91001~91004）")
    void intI01_pageQuery_returnsAtLeast4Records() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(4)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-I-02 新增意向-返回ID-DB草稿状态
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-I-02 新增意向-返回Long ID-DB中status=0")
    void intI02_createIntention_returnIdAndDraftInDB() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("intentionName", "IT测试意向");
        body.put("projectId", 90001);
        body.put("contractStart", "2026-06-01");
        body.put("contractEnd", "2028-05-31");
        body.put("paymentCycle", 3);
        body.put("billingMode", 1);

        String resp = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        long newId = objectMapper.readTree(resp).path("data").longValue();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_intention WHERE id=?", Integer.class, newId);
        assertEquals(0, status, "新建意向 status 应为草稿(0)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-I-03 发起审批-草稿→审批中
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-I-03 发起审批-91001草稿→DB中status=1")
    void intI03_submitApproval_91001_statusBecomesApproving() throws Exception {
        mockMvc.perform(post(BASE_URL + "/91001/submit-approval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_intention WHERE id=91001", Integer.class);
        assertEquals(1, status, "91001 status 应变为审批中(1)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-I-04 审批通过回调-状态变2
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-I-04 审批通过回调-91002(审批中)→DB中status=2")
    void intI04_approvalCallbackApproved_91002_statusBecomesApproved() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approved", true);

        mockMvc.perform(post(BASE_URL + "/91002/approval-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_intention WHERE id=91002", Integer.class);
        assertEquals(2, status, "91002 status 应变为审批通过(2)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-I-05 审批驳回回调-状态变3
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-I-05 审批驳回回调-91002(审批中)→DB中status=3")
    void intI05_approvalCallbackRejected_91002_statusBecomesRejected() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approved", false);

        mockMvc.perform(post(BASE_URL + "/91002/approval-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_intention WHERE id=91002", Integer.class);
        assertEquals(3, status, "91002 status 应变为驳回(3)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-I-06 删除草稿意向-成功
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-I-06 删除草稿意向-91001-DB中is_deleted=1")
    void intI06_deleteIntention_91001Draft_isDeletedBecomesOne() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/91001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer isDeleted = jdbc.queryForObject(
                "SELECT is_deleted FROM inv_intention WHERE id=91001", Integer.class);
        assertEquals(1, isDeleted, "91001 逻辑删除后 is_deleted 应为 1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-I-07 删除审批中意向-业务异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-I-07 删除审批中意向-91002-返回业务错误且DB中is_deleted仍为0")
    void intI07_deleteIntention_91002Approving_returnsBizErrorAndNotDeleted() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/91002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));

        Integer isDeleted = jdbc.queryForObject(
                "SELECT is_deleted FROM inv_intention WHERE id=91002", Integer.class);
        assertEquals(0, isDeleted, "91002 审批中不应被删除");
    }
}
