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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 楼栋管理接口集成测试（BLDG-I-01 ~ BLDG-I-05）
 * 使用永久测试数据：
 *   楼栋90001（A座，有楼层，不可删除）
 *   楼栋90002（B座，无楼层无商铺，可删除）
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
@DisplayName("楼栋管理接口集成测试")
class BizBuildingControllerIT {

    private static final String BASE_URL = "/base/buildings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-I-01 分页查询-按项目过滤
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-I-01 分页查询-projectId=90001-返回至少2条楼栋（A座+B座）")
    void listBuildings_byProject_returnsAtLeastTwo() throws Exception {
        mockMvc.perform(get(BASE_URL).param("projectId", "90001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-I-02 新增楼栋-合法数据
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-I-02 新增楼栋-合法数据-返回200并写入数据库")
    void createBuilding_validData_returnsIdAndPersists() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001);
        body.put("buildingCode", "C-TEST-IT");
        body.put("buildingName", "集成测试楼栋");
        body.put("status", 1);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_building WHERE building_code='C-TEST-IT' AND is_deleted=0",
                Integer.class);
        assertEquals(1, count, "新增楼栋应在DB中存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-I-03 新增楼栋-编码重复
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-I-03 新增楼栋-编码BLDG-90001已存在-返回业务异常500")
    void createBuilding_duplicateCode_returnsBizError() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001);
        body.put("buildingCode", "BLDG-90001"); // 永久数据中已存在
        body.put("buildingName", "重复编码楼栋");
        body.put("status", 1);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-I-04 删除楼栋-有楼层关联-拒绝删除
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-I-04 删除楼栋-A座(90001)有楼层-返回500含楼层")
    void deleteBuilding_hasFloors_returnsBizError() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 90001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("楼层")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-I-05 删除楼栋-无关联-成功
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-I-05 删除楼栋-B座(90002)无楼层-返回200")
    void deleteBuilding_noAssociation_success() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 90002L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
