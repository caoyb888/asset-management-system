package com.asset.investment.contract.controller;

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
 * 招商合同 Controller 集成测试（CTR-I-01 ~ CTR-I-06）
 * 依赖永久测试数据 inv_lease_contract(91001/91002/91003)
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
@DisplayName("招商合同 Controller 集成测试")
class InvLeaseContractControllerIT {

    private static final String BASE_URL = "/inv/contracts";

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-I-01 分页查询合同列表（仅当前版本）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-I-01 分页查询合同列表-仅is_current=1-总数>=3")
    void ctrI01_pageQuery_currentVersionOnly_atLeast3Records() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(3)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-I-02 新增合同-草稿状态
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-I-02 新增合同-返回Long ID-DB中status=0")
    void ctrI02_createContract_returnIdAndDraftInDB() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contractName", "IT测试合同");
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
                "SELECT status FROM inv_lease_contract WHERE id=?", Integer.class, newId);
        assertEquals(0, status, "新建合同 status 应为草稿(0)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-I-03 发起审批-草稿→审批中
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-I-03 发起审批-91001草稿→DB中status=1")
    void ctrI03_submitApproval_91001_statusBecomesApproving() throws Exception {
        mockMvc.perform(post(BASE_URL + "/91001/submit-approval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_lease_contract WHERE id=91001", Integer.class);
        assertEquals(1, status, "91001 status 应变为审批中(1)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-I-04 审批通过-状态变2且写版本快照
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-I-04 审批通过回调-91002(审批中)→status=2且写版本快照")
    void ctrI04_approvalCallbackApproved_91002_statusEffectiveAndVersionCreated() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approved", true);

        mockMvc.perform(post(BASE_URL + "/91002/approval-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM inv_lease_contract WHERE id=91002", Integer.class);
        assertEquals(2, status, "91002 status 应变为生效(2)");

        Integer versionCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inv_lease_contract_version WHERE contract_id=91002",
                Integer.class);
        assertEquals(true, versionCount >= 1, "审批通过后应写入版本快照");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-I-05 删除草稿合同-成功
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-I-05 删除草稿合同-91001-DB中is_deleted=1")
    void ctrI05_deleteContract_91001Draft_isDeletedBecomesOne() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/91001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer isDeleted = jdbc.queryForObject(
                "SELECT is_deleted FROM inv_lease_contract WHERE id=91001", Integer.class);
        assertEquals(1, isDeleted, "91001 逻辑删除后 is_deleted 应为 1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CTR-I-06 删除审批中合同-业务异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CTR-I-06 删除审批中合同-91002-返回业务错误且DB中is_deleted仍为0")
    void ctrI06_deleteContract_91002Approving_returnsBizErrorAndNotDeleted() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/91002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));

        Integer isDeleted = jdbc.queryForObject(
                "SELECT is_deleted FROM inv_lease_contract WHERE id=91002", Integer.class);
        assertEquals(0, isDeleted, "91002 审批中不应被删除");
    }
}
