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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 楼层管理接口集成测试（FLR-I-01 ~ FLR-I-04）
 * 永久测试数据：
 *   楼层90001（1F，有商铺A101/A102/A103，不可删除）
 *   楼层90002（2F，有商铺A201，不可删除）
 * FLR-I-04 使用 @BeforeEach 临时创建无商铺楼层
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
@DisplayName("楼层管理接口集成测试")
class BizFloorControllerIT {

    private static final String BASE_URL = "/base/floors";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 临时创建的无商铺楼层ID，用于 FLR-I-04 */
    private Long emptyFloorId;

    @BeforeEach
    void setUp() {
        // 创建一个无商铺的临时楼层（随事务回滚）
        jdbc.update("""
                INSERT INTO biz_floor
                    (project_id, building_id, floor_code, floor_name, status,
                     is_deleted, created_by, updated_by, created_at, updated_at)
                VALUES (90001, 90001, 'FLR-EMPTY-IT', '临时空楼层', 1,
                        0, 0, 0, NOW(), NOW())
                """);
        emptyFloorId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-I-01 按楼栋分页查询
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-I-01 按楼栋分页查询-buildingId=90001-返回至少2条（1F+2F+临时楼层）")
    void listFloors_byBuilding_returnsAtLeastTwo() throws Exception {
        mockMvc.perform(get(BASE_URL).param("buildingId", "90001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 1F(90001) + 2F(90002) + 临时空楼层 = 至少3条
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-I-02 新增楼层-合法
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-I-02 新增楼层-合法数据-返回200并写入数据库")
    void createFloor_validData_returnsIdAndPersists() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", 90001);
        body.put("buildingId", 90001);
        body.put("floorCode", "FLR-NEW-IT");
        body.put("floorName", "集成测试楼层");
        body.put("status", 1);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_floor WHERE floor_code='FLR-NEW-IT' AND is_deleted=0",
                Integer.class);
        assertEquals(1, count, "新增楼层应在DB中存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-I-03 删除有商铺的楼层-拒绝
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-I-03 删除楼层-1F(90001)有商铺-返回500含商铺")
    void deleteFloor_hasShops_returnsBizError() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 90001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("商铺")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-I-04 删除无商铺的楼层-成功
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-I-04 删除楼层-临时空楼层(无商铺)-返回200")
    void deleteFloor_emptyFloor_success() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", emptyFloorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
