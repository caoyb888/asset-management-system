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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 商铺管理接口集成测试（SHOP-I-01 ~ SHOP-I-05）
 * 永久测试数据：
 *   商铺90001（A101, 300㎡, floorId=90001, 用于拆分）
 *   商铺90002（A102, 100㎡, floorId=90001, 用于合并）
 *   商铺90003（A103, 100㎡, floorId=90001, 用于合并）
 *   商铺90004（A201, 200㎡, floorId=90002, 不同楼层，用于跨楼层校验）
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
@DisplayName("商铺管理接口集成测试")
class BizShopControllerIT {

    private static final String BASE_URL = "/base/shops";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // SHOP-I-01 分页查询商铺
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHOP-I-01 分页查询-projectId=90001-返回至少4条")
    void listShops_byProject_returnsAtLeastFour() throws Exception {
        mockMvc.perform(get(BASE_URL).param("projectId", "90001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(4)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHOP-I-02 拆分商铺 A101(90001) → 两个子商铺
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHOP-I-02 拆分A101(90001)-成功-源铺is_deleted=1，新铺存在")
    void splitShop_A101_success() throws Exception {
        Map<String, Object> sub1 = new HashMap<>();
        sub1.put("shopCode", "A101-L");
        sub1.put("shopType", 1);
        sub1.put("rentArea", 150.00);

        Map<String, Object> sub2 = new HashMap<>();
        sub2.put("shopCode", "A101-R");
        sub2.put("shopType", 2);
        sub2.put("rentArea", 150.00);

        Map<String, Object> body = new HashMap<>();
        body.put("sourceShopId", 90001L);
        body.put("subShops", List.of(sub1, sub2));
        body.put("remark", "集成测试拆分");

        mockMvc.perform(post(BASE_URL + "/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证源商铺已逻辑删除
        Integer srcDeleted = jdbc.queryForObject(
                "SELECT is_deleted FROM biz_shop WHERE id=90001", Integer.class);
        assertEquals(1, srcDeleted, "源商铺应被逻辑删除");

        // 验证子商铺已创建
        Integer subCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_shop WHERE shop_code IN ('A101-L','A101-R') AND is_deleted=0",
                Integer.class);
        assertEquals(2, subCount, "子商铺应已创建");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHOP-I-03 合并 A102(90002)+A103(90003)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHOP-I-03 合并A102+A103-成功-源铺is_deleted=1，新铺存在")
    void mergeShop_A102_A103_success() throws Exception {
        Map<String, Object> newShop = new HashMap<>();
        newShop.put("shopCode", "A102-MERGED");
        newShop.put("shopType", 2);
        newShop.put("rentArea", 200.00);

        Map<String, Object> body = new HashMap<>();
        body.put("sourceShopIds", List.of(90002L, 90003L));
        body.put("newShop", newShop);
        body.put("remark", "集成测试合并");

        mockMvc.perform(post(BASE_URL + "/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证源商铺已逻辑删除
        Integer srcCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_shop WHERE id IN (90002,90003) AND is_deleted=1",
                Integer.class);
        assertEquals(2, srcCount, "源商铺应均被逻辑删除");

        // 验证新商铺已创建
        Integer newCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_shop WHERE shop_code='A102-MERGED' AND is_deleted=0",
                Integer.class);
        assertEquals(1, newCount, "合并后新商铺应已创建");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHOP-I-04 拆分-面积不守恒
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHOP-I-04 拆分A101-子铺面积不守恒-返回500含面积不一致")
    void splitShop_areaNotConserved_returnsBizError() throws Exception {
        Map<String, Object> sub1 = new HashMap<>();
        sub1.put("shopCode", "A101-X1");
        sub1.put("rentArea", 100.00);

        Map<String, Object> sub2 = new HashMap<>();
        sub2.put("shopCode", "A101-X2");
        sub2.put("rentArea", 100.00); // 合计200，源铺300，差100 > 0.01

        Map<String, Object> body = new HashMap<>();
        body.put("sourceShopId", 90001L);
        body.put("subShops", List.of(sub1, sub2));

        mockMvc.perform(post(BASE_URL + "/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("面积")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHOP-I-05 合并-跨楼层（A102 + A201 不同楼层）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHOP-I-05 合并跨楼层(A102+A201)-返回500含同一楼层")
    void mergeShop_crossFloor_returnsBizError() throws Exception {
        Map<String, Object> newShop = new HashMap<>();
        newShop.put("shopCode", "CROSS-MERGED");
        newShop.put("rentArea", 300.00);

        Map<String, Object> body = new HashMap<>();
        body.put("sourceShopIds", List.of(90002L, 90004L)); // A102(1F) + A201(2F)
        body.put("newShop", newShop);

        mockMvc.perform(post(BASE_URL + "/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("同一楼层")));
    }
}
