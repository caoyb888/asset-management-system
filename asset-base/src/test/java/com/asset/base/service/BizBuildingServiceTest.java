package com.asset.base.service;

import com.asset.base.converter.BuildingConverter;
import com.asset.base.entity.BizBuilding;
import com.asset.base.mapper.BizBuildingMapper;
import com.asset.base.mapper.BizFloorMapper;
import com.asset.base.mapper.BizShopMapper;
import com.asset.base.model.dto.BuildingSaveDTO;
import com.asset.base.model.vo.BuildingVO;
import com.asset.base.service.impl.BizBuildingServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 楼栋管理 Service 单元测试（BLDG-U-01 ~ BLDG-U-08）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("楼栋管理 Service 单元测试")
class BizBuildingServiceTest {

    @Mock
    BizBuildingMapper buildingMapper;

    @Mock
    BuildingConverter converter;

    @Mock
    BizFloorMapper floorMapper;

    @Mock
    BizShopMapper shopMapper;

    @Spy
    @InjectMocks
    BizBuildingServiceImpl buildingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(buildingService, "baseMapper", buildingMapper);
        ReflectionTestUtils.setField(buildingService, "floorMapper", floorMapper);
        ReflectionTestUtils.setField(buildingService, "shopMapper", shopMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-01 新增-编码唯一校验通过
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-01 新增楼栋-编码唯一-调用save")
    void createBuilding_uniqueCode_callsSave() {
        BuildingSaveDTO dto = new BuildingSaveDTO();
        dto.setProjectId(1L);
        dto.setBuildingCode("B-NEW");
        dto.setBuildingName("新楼栋");

        BizBuilding entity = new BizBuilding();
        entity.setId(100L);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(0L).when(buildingService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(buildingService).save(entity);

        Long id = buildingService.createBuilding(dto);

        assertThat(id).isEqualTo(100L);
        verify(buildingService).save(entity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-02 新增-编码重复
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-02 新增楼栋-编码重复-抛出BizException")
    void createBuilding_duplicateCode_throwsBizException() {
        BuildingSaveDTO dto = new BuildingSaveDTO();
        dto.setProjectId(1L);
        dto.setBuildingCode("B-DUP");
        dto.setBuildingName("重复楼栋");

        doReturn(1L).when(buildingService).count(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> buildingService.createBuilding(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-03 新增-编码为空跳过唯一性校验
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-03 新增楼栋-编码为空-跳过唯一性校验")
    void createBuilding_blankCode_skipUniquenessCheck() {
        BuildingSaveDTO dto = new BuildingSaveDTO();
        dto.setProjectId(1L);
        dto.setBuildingCode(null); // 编码为空
        dto.setBuildingName("无编码楼栋");

        BizBuilding entity = new BizBuilding();
        entity.setId(100L);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(true).when(buildingService).save(entity);

        buildingService.createBuilding(dto);

        // 编码为空时不应调用 count 做唯一性检查
        verify(buildingService, never()).count(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-04 删除-有楼层关联
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-04 删除楼栋-有楼层关联-抛出BizException含楼层数")
    void deleteBuilding_hasFloors_throwsBizException() {
        BizBuilding existing = new BizBuilding();
        existing.setId(1L);
        existing.setIsDeleted(0);

        doReturn(existing).when(buildingService).getById(1L);
        // 有 2 个楼层
        when(floorMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> buildingService.deleteBuilding(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("2")
                .hasMessageContaining("楼层");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-05 删除-有商铺关联
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-05 删除楼栋-有商铺关联-抛出BizException含商铺数")
    void deleteBuilding_hasShops_throwsBizException() {
        BizBuilding existing = new BizBuilding();
        existing.setId(1L);
        existing.setIsDeleted(0);

        doReturn(existing).when(buildingService).getById(1L);
        // 楼层 0 个，但商铺 3 个
        when(floorMapper.selectCount(any())).thenReturn(0L);
        when(shopMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> buildingService.deleteBuilding(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("3")
                .hasMessageContaining("商铺");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-06 删除-无关联成功
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-06 删除楼栋-无关联-调用removeById")
    void deleteBuilding_noAssociation_callsRemoveById() {
        BizBuilding existing = new BizBuilding();
        existing.setId(1L);
        existing.setIsDeleted(0);

        doReturn(existing).when(buildingService).getById(1L);
        when(floorMapper.selectCount(any())).thenReturn(0L);
        when(shopMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(buildingService).removeById(1L);

        buildingService.deleteBuilding(1L);

        verify(buildingService).removeById(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-07 编辑-编码未变不重复校验
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-07 编辑楼栋-编码与原值相同-跳过唯一性校验")
    void updateBuilding_sameCode_skipCheck() {
        BizBuilding existing = new BizBuilding();
        existing.setId(1L);
        existing.setBuildingCode("B-SAME");
        existing.setIsDeleted(0);

        BuildingSaveDTO dto = new BuildingSaveDTO();
        dto.setBuildingCode("B-SAME"); // 未变

        doReturn(existing).when(buildingService).getById(1L);
        doReturn(true).when(buildingService).updateById(any());

        buildingService.updateBuilding(1L, dto);

        verify(buildingService, never()).count(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLDG-U-08 枚举名称填充
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BLDG-U-08 枚举名称填充-status=0返回停用")
    void getBuildingById_enumNameFilledCorrectly() {
        BizBuilding building = new BizBuilding();
        building.setId(1L);
        building.setIsDeleted(0);

        BuildingVO vo = new BuildingVO();
        vo.setStatus(0);

        doReturn(building).when(buildingService).getById(1L);
        when(converter.toVO(building)).thenReturn(vo);

        BuildingVO result = buildingService.getBuildingById(1L);

        assertThat(result.getStatusName()).isEqualTo("停用");
    }
}
