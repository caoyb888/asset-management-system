package com.asset.base.service;

import com.asset.base.converter.ShopConverter;
import com.asset.base.entity.BizShop;
import com.asset.base.entity.BizShopRelation;
import com.asset.base.mapper.BizShopMapper;
import com.asset.base.mapper.BizShopRelationMapper;
import com.asset.base.model.dto.ShopMergeDTO;
import com.asset.base.model.dto.ShopSaveDTO;
import com.asset.base.model.dto.ShopSplitDTO;
import com.asset.base.service.impl.BizShopServiceImpl;
import com.asset.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商铺管理 Service 单元测试（SHOP-U-01~U-03, SHOP-SPL-01~SPL-08, SHOP-MGE-01~MGE-07）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商铺管理 Service 单元测试")
class BizShopServiceTest {

    @Mock
    BizShopMapper shopMapper;

    @Mock
    ShopConverter converter;

    @Mock
    BizShopRelationMapper shopRelationMapper;

    @Spy
    @InjectMocks
    BizShopServiceImpl shopService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shopService, "baseMapper", shopMapper);
    }

    /* ====================================================================
       SHOP-U-01 ~ SHOP-U-03  基础 CRUD
       ==================================================================== */

    @Test
    @DisplayName("SHOP-U-01 新增商铺-编码唯一-调用save")
    void createShop_uniqueCode_callsSave() {
        ShopSaveDTO dto = new ShopSaveDTO();
        dto.setProjectId(1L);
        dto.setShopCode("SHP-NEW");

        BizShop entity = new BizShop();
        entity.setId(200L);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).save(entity);

        shopService.createShop(dto);

        verify(shopService).save(entity);
    }

    @Test
    @DisplayName("SHOP-U-02 新增商铺-编码重复-抛出BizException")
    void createShop_duplicateCode_throws() {
        ShopSaveDTO dto = new ShopSaveDTO();
        dto.setProjectId(1L);
        dto.setShopCode("A101");

        doReturn(1L).when(shopService).count(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> shopService.createShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("SHOP-U-03 编辑商铺-编码未变更-跳过唯一性校验")
    void updateShop_sameCode_skipCheck() {
        BizShop existing = new BizShop();
        existing.setId(1L);
        existing.setProjectId(1L);
        existing.setShopCode("SHP-SAME");
        existing.setIsDeleted(0);

        ShopSaveDTO dto = new ShopSaveDTO();
        dto.setShopCode("SHP-SAME"); // 未变

        doReturn(existing).when(shopService).getById(1L);
        doReturn(true).when(shopService).updateById(any());

        shopService.updateShop(1L, dto);

        verify(shopService, never()).count(any());
    }

    /* ====================================================================
       SHOP-SPL-01 ~ SHOP-SPL-08  商铺拆分
       ==================================================================== */

    @Test
    @DisplayName("SHOP-SPL-01 拆分-面积守恒校验通过-save 3次，关系记录 3次")
    void splitShop_areaConservation_success() {
        BizShop source = buildShop(1L, 1L, 1L, 1L, "SRC-01", new BigDecimal("300.00"));

        doReturn(source).when(shopService).getById(1L);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeById(1L);
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        ShopSplitDTO dto = buildSplitDTO(1L,
                sub("S-01", new BigDecimal("100.00")),
                sub("S-02", new BigDecimal("100.00")),
                sub("S-03", new BigDecimal("100.00")));

        shopService.splitShop(dto);

        verify(shopService, times(3)).save(any(BizShop.class));
        verify(shopRelationMapper, times(3)).insert(any(BizShopRelation.class));
    }

    @Test
    @DisplayName("SHOP-SPL-02 拆分-面积差值>0.01-抛出BizException")
    void splitShop_areaNotConserved_throws() {
        BizShop source = buildShop(1L, 1L, 1L, 1L, "SRC-02", new BigDecimal("300.00"));
        doReturn(source).when(shopService).getById(1L);

        ShopSplitDTO dto = buildSplitDTO(1L,
                sub("S-01", new BigDecimal("100.00")),
                sub("S-02", new BigDecimal("100.00")),
                sub("S-03", new BigDecimal("99.00"))); // 总和 299，差 1 > 0.01

        assertThatThrownBy(() -> shopService.splitShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("面积");
    }

    @Test
    @DisplayName("SHOP-SPL-03 拆分-面积差值≤0.01-正常通过（容差内）")
    void splitShop_areaDiffWithinTolerance_success() {
        BizShop source = buildShop(1L, 1L, 1L, 1L, "SRC-03", new BigDecimal("300.00"));

        doReturn(source).when(shopService).getById(1L);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeById(1L);
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        // 总和 300.005，差 0.005 ≤ 0.01
        ShopSplitDTO dto = buildSplitDTO(1L,
                sub("S-01", new BigDecimal("100.000")),
                sub("S-02", new BigDecimal("100.000")),
                sub("S-03", new BigDecimal("100.005")));

        shopService.splitShop(dto); // 不应抛出异常
    }

    @Test
    @DisplayName("SHOP-SPL-04 拆分-子商铺编码重复-抛出BizException")
    void splitShop_subCodeConflict_throws() {
        BizShop source = buildShop(1L, 1L, 1L, 1L, "SRC-04", new BigDecimal("300.00"));
        doReturn(source).when(shopService).getById(1L);
        // 第一个子铺编码校验时返回已存在
        doReturn(1L).when(shopService).count(any(LambdaQueryWrapper.class));

        ShopSplitDTO dto = buildSplitDTO(1L,
                sub("DUP-CODE", new BigDecimal("150.00")),
                sub("S-02", new BigDecimal("150.00")));

        assertThatThrownBy(() -> shopService.splitShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("SHOP-SPL-05 拆分-源商铺不存在-抛出BizException")
    void splitShop_sourceNotFound_throws() {
        doReturn(null).when(shopService).getById(999999L);

        ShopSplitDTO dto = buildSplitDTO(999999L,
                sub("S-01", new BigDecimal("100.00")),
                sub("S-02", new BigDecimal("100.00")));

        assertThatThrownBy(() -> shopService.splitShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("SHOP-SPL-06 拆分成功-源商铺被逻辑删除")
    void splitShop_success_sourceIsDeleted() {
        BizShop source = buildShop(1L, 1L, 1L, 1L, "SRC-06", new BigDecimal("200.00"));

        doReturn(source).when(shopService).getById(1L);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeById(1L);
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        ShopSplitDTO dto = buildSplitDTO(1L,
                sub("S-01", new BigDecimal("100.00")),
                sub("S-02", new BigDecimal("100.00")));

        shopService.splitShop(dto);

        verify(shopService).removeById(1L);
    }

    @Test
    @DisplayName("SHOP-SPL-07 拆分关系记录-relationType=1")
    void splitShop_relationRecords_typeIs1() {
        BizShop source = buildShop(1L, 1L, 1L, 1L, "SRC-07", new BigDecimal("200.00"));

        doReturn(source).when(shopService).getById(1L);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeById(1L);
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        ShopSplitDTO dto = buildSplitDTO(1L,
                sub("S-01", new BigDecimal("100.00")),
                sub("S-02", new BigDecimal("100.00")));

        shopService.splitShop(dto);

        verify(shopRelationMapper, times(2)).insert(argThat(
                (BizShopRelation r) -> r.getRelationType() == 1));
    }

    @Test
    @DisplayName("SHOP-SPL-08 拆分-源面积为null-跳过守恒校验")
    void splitShop_nullSourceArea_skipCheck() {
        BizShop source = buildShop(1L, 1L, 1L, 1L, "SRC-08", null); // rentArea=null

        doReturn(source).when(shopService).getById(1L);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeById(1L);
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        ShopSplitDTO dto = buildSplitDTO(1L,
                sub("S-01", new BigDecimal("100.00")),
                sub("S-02", new BigDecimal("50.00"))); // 面积不一致，但因源为null跳过校验

        shopService.splitShop(dto); // 不应抛出异常
    }

    /* ====================================================================
       SHOP-MGE-01 ~ SHOP-MGE-07  商铺合并
       ==================================================================== */

    @Test
    @DisplayName("SHOP-MGE-01 合并成功-save 1次，关系记录 3次")
    void mergeShop_success() {
        List<BizShop> sources = List.of(
                buildShop(1L, 1L, 1L, 1L, "A102", new BigDecimal("100.00")),
                buildShop(2L, 1L, 1L, 1L, "A103", new BigDecimal("100.00")),
                buildShop(3L, 1L, 1L, 1L, "A104", new BigDecimal("100.00")));

        ShopMergeDTO dto = buildMergeDTO(
                List.of(1L, 2L, 3L),
                mergedShop("NEW-A", new BigDecimal("300.00")));

        doReturn(sources).when(shopService).listByIds(List.of(1L, 2L, 3L));
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeByIds(List.of(1L, 2L, 3L));
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        shopService.mergeShop(dto);

        verify(shopService, times(1)).save(any(BizShop.class));
        verify(shopRelationMapper, times(3)).insert(any(BizShopRelation.class));
    }

    @Test
    @DisplayName("SHOP-MGE-02 合并-不同楼层-抛出BizException")
    void mergeShop_differentFloors_throws() {
        List<BizShop> sources = List.of(
                buildShop(1L, 1L, 1L, 1L, "A102", new BigDecimal("100.00")),
                buildShop(2L, 1L, 1L, 2L, "A201", new BigDecimal("100.00"))); // 不同楼层

        ShopMergeDTO dto = buildMergeDTO(
                List.of(1L, 2L),
                mergedShop("NEW-B", new BigDecimal("200.00")));

        doReturn(sources).when(shopService).listByIds(List.of(1L, 2L));

        assertThatThrownBy(() -> shopService.mergeShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("同一楼层");
    }

    @Test
    @DisplayName("SHOP-MGE-03 合并-面积不守恒-抛出BizException")
    void mergeShop_areaNotConserved_throws() {
        List<BizShop> sources = List.of(
                buildShop(1L, 1L, 1L, 1L, "A102", new BigDecimal("100.00")),
                buildShop(2L, 1L, 1L, 1L, "A103", new BigDecimal("100.00")),
                buildShop(3L, 1L, 1L, 1L, "A104", new BigDecimal("100.00")));

        ShopMergeDTO dto = buildMergeDTO(
                List.of(1L, 2L, 3L),
                mergedShop("NEW-C", new BigDecimal("290.00"))); // 差10 > 0.01

        doReturn(sources).when(shopService).listByIds(List.of(1L, 2L, 3L));

        assertThatThrownBy(() -> shopService.mergeShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("面积");
    }

    @Test
    @DisplayName("SHOP-MGE-04 合并-源商铺部分不存在-抛出BizException")
    void mergeShop_sourcePartialNotFound_throws() {
        // listByIds 只返回1条，但传入2个ID
        List<BizShop> sources = List.of(
                buildShop(1L, 1L, 1L, 1L, "A102", new BigDecimal("100.00")));

        ShopMergeDTO dto = buildMergeDTO(
                List.of(1L, 999999L), // 999999 不存在
                mergedShop("NEW-D", new BigDecimal("200.00")));

        doReturn(sources).when(shopService).listByIds(List.of(1L, 999999L));

        assertThatThrownBy(() -> shopService.mergeShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("SHOP-MGE-05 合并-新铺编码重复-抛出BizException")
    void mergeShop_newCodeConflict_throws() {
        List<BizShop> sources = List.of(
                buildShop(1L, 1L, 1L, 1L, "A102", new BigDecimal("100.00")),
                buildShop(2L, 1L, 1L, 1L, "A103", new BigDecimal("100.00")));

        ShopMergeDTO dto = buildMergeDTO(
                List.of(1L, 2L),
                mergedShop("DUP-SHOP", new BigDecimal("200.00")));

        doReturn(sources).when(shopService).listByIds(List.of(1L, 2L));
        doReturn(1L).when(shopService).count(any(LambdaQueryWrapper.class)); // 新编码已存在

        assertThatThrownBy(() -> shopService.mergeShop(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("SHOP-MGE-06 合并成功-源商铺全部逻辑删除")
    void mergeShop_success_sourcesAreDeleted() {
        List<Long> sourceIds = List.of(1L, 2L);
        List<BizShop> sources = List.of(
                buildShop(1L, 1L, 1L, 1L, "A102", new BigDecimal("100.00")),
                buildShop(2L, 1L, 1L, 1L, "A103", new BigDecimal("100.00")));

        ShopMergeDTO dto = buildMergeDTO(sourceIds, mergedShop("NEW-F", new BigDecimal("200.00")));

        doReturn(sources).when(shopService).listByIds(sourceIds);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeByIds(sourceIds);
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        shopService.mergeShop(dto);

        verify(shopService).removeByIds(sourceIds);
    }

    @Test
    @DisplayName("SHOP-MGE-07 合并关系记录-relationType=2")
    void mergeShop_relationRecords_typeIs2() {
        List<Long> sourceIds = List.of(1L, 2L);
        List<BizShop> sources = List.of(
                buildShop(1L, 1L, 1L, 1L, "A102", new BigDecimal("100.00")),
                buildShop(2L, 1L, 1L, 1L, "A103", new BigDecimal("100.00")));

        ShopMergeDTO dto = buildMergeDTO(sourceIds, mergedShop("NEW-G", new BigDecimal("200.00")));

        doReturn(sources).when(shopService).listByIds(sourceIds);
        doReturn(0L).when(shopService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(shopService).removeByIds(sourceIds);
        doReturn(true).when(shopService).save(any(BizShop.class));
        when(shopRelationMapper.insert(any(BizShopRelation.class))).thenReturn(1);

        shopService.mergeShop(dto);

        verify(shopRelationMapper, times(2)).insert(argThat(
                (BizShopRelation r) -> r.getRelationType() == 2));
    }

    /* ====================================================================
       辅助方法
       ==================================================================== */

    private BizShop buildShop(Long id, Long projectId, Long buildingId, Long floorId,
                               String shopCode, BigDecimal rentArea) {
        BizShop shop = new BizShop();
        shop.setId(id);
        shop.setProjectId(projectId);
        shop.setBuildingId(buildingId);
        shop.setFloorId(floorId);
        shop.setShopCode(shopCode);
        shop.setRentArea(rentArea);
        shop.setIsDeleted(0);
        return shop;
    }

    private ShopSplitDTO buildSplitDTO(Long sourceId, ShopSplitDTO.SubShopDTO... subs) {
        ShopSplitDTO dto = new ShopSplitDTO();
        dto.setSourceShopId(sourceId);
        dto.setSubShops(List.of(subs));
        return dto;
    }

    private ShopSplitDTO.SubShopDTO sub(String shopCode, BigDecimal rentArea) {
        ShopSplitDTO.SubShopDTO sub = new ShopSplitDTO.SubShopDTO();
        sub.setShopCode(shopCode);
        sub.setRentArea(rentArea);
        return sub;
    }

    private ShopMergeDTO buildMergeDTO(List<Long> sourceIds, ShopMergeDTO.MergedShopDTO newShop) {
        ShopMergeDTO dto = new ShopMergeDTO();
        dto.setSourceShopIds(sourceIds);
        dto.setNewShop(newShop);
        return dto;
    }

    private ShopMergeDTO.MergedShopDTO mergedShop(String shopCode, BigDecimal rentArea) {
        ShopMergeDTO.MergedShopDTO merged = new ShopMergeDTO.MergedShopDTO();
        merged.setShopCode(shopCode);
        merged.setRentArea(rentArea);
        return merged;
    }
}
