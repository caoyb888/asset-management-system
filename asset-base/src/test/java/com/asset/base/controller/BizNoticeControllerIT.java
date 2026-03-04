package com.asset.base.controller;

import com.asset.base.BaseApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 通知公告接口集成测试（NTC-I-01 ~ NTC-I-07）
 * 永久测试数据：
 *   公告90001（已发布，用于发布/下架/已读接口测试）
 *   公告90002（草稿，用于发布时间自动填充测试）
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
@DisplayName("通知公告接口集成测试")
class BizNoticeControllerIT {

    private static final String BASE_URL = "/base/notices";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-I-01 分页-类型过滤
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-I-01 分页查询-noticeType=1(通知)-只返回通知类型记录")
    void listNotices_filterByType_returnsMatchedOnly() throws Exception {
        mockMvc.perform(get(BASE_URL).param("noticeType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[*].noticeType",
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.equalTo(1))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-I-02 分页-状态过滤
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-I-02 分页查询-status=1(已发布)-只返回已发布记录")
    void listNotices_filterByStatus_returnsMatchedOnly() throws Exception {
        mockMvc.perform(get(BASE_URL).param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[*].status",
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.equalTo(1))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-I-03 新增草稿-publish_time为NULL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-I-03 新增草稿公告-publish_time应为NULL")
    void createNotice_draft_publishTimeIsNull() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "集成测试草稿");
        body.put("noticeType", 1);
        body.put("status", 0); // 草稿

        String responseStr = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long newId = objectMapper.readTree(responseStr).path("data").longValue();

        Object publishTime = jdbc.queryForObject(
                "SELECT publish_time FROM biz_notice WHERE id=?", Object.class, newId);
        assertNull(publishTime, "草稿公告的 publish_time 应为 NULL");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-I-04 新增直接发布-publish_time不为NULL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-I-04 新增直接发布公告-publish_time应不为NULL")
    void createNotice_published_publishTimeIsNotNull() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "集成测试直接发布");
        body.put("noticeType", 2);
        body.put("status", 1); // 直接发布

        String responseStr = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long newId = objectMapper.readTree(responseStr).path("data").longValue();

        Object publishTime = jdbc.queryForObject(
                "SELECT publish_time FROM biz_notice WHERE id=?", Object.class, newId);
        assertNotNull(publishTime, "直接发布公告的 publish_time 不应为 NULL");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-I-05 发布公告
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-I-05 发布公告90002(草稿)-DB中status=1")
    void publishNotice_updatesStatusToOne() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/publish", 90002L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM biz_notice WHERE id=90002", Integer.class);
        assertEquals(1, status, "发布后公告 status 应为 1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-I-06 下架公告
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-I-06 下架公告90001(已发布)-DB中status=2")
    void unpublishNotice_updatesStatusToTwo() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/unpublish", 90001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM biz_notice WHERE id=90001", Integer.class);
        assertEquals(2, status, "下架后公告 status 应为 2");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-I-07 标记已读-幂等（调用2次，第2次仍200）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-I-07 标记已读-重复调用2次-第2次仍返回code=200")
    void markAsRead_idempotent_secondCallStillSuccess() throws Exception {
        // 第一次标记已读
        mockMvc.perform(post(BASE_URL + "/{id}/read", 90001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 第二次标记已读（幂等，不应报错）
        mockMvc.perform(post(BASE_URL + "/{id}/read", 90001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
