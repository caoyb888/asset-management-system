package com.asset.base.service;

import com.asset.base.converter.FloorConverter;
import com.asset.base.entity.BizFloor;
import com.asset.base.mapper.BizFloorMapper;
import com.asset.base.mapper.BizShopMapper;
import com.asset.base.model.dto.FloorSaveDTO;
import com.asset.base.model.vo.FloorVO;
import com.asset.base.service.impl.BizFloorServiceImpl;
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
 * 楼层管理 Service 单元测试（FLR-U-01 ~ FLR-U-05）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("楼层管理 Service 单元测试")
class BizFloorServiceTest {

    @Mock
    BizFloorMapper floorMapper;

    @Mock
    FloorConverter converter;

    @Mock
    BizShopMapper shopMapper;

    @Spy
    @InjectMocks
    BizFloorServiceImpl floorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(floorService, "baseMapper", floorMapper);
        ReflectionTestUtils.setField(floorService, "shopMapper", shopMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-U-01 新增-编码唯一校验通过
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-U-01 新增楼层-编码唯一-调用save")
    void createFloor_uniqueCode_callsSave() {
        FloorSaveDTO dto = new FloorSaveDTO();
        dto.setBuildingId(1L);
        dto.setProjectId(1L);
        dto.setFloorCode("F-NEW");
        dto.setFloorName("新楼层");

        BizFloor entity = new BizFloor();
        entity.setId(100L);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(0L).when(floorService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(floorService).save(entity);

        Long id = floorService.createFloor(dto);

        assertThat(id).isEqualTo(100L);
        verify(floorService).save(entity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-U-02 新增-同楼栋内编码重复
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-U-02 新增楼层-同楼栋内编码重复-抛出BizException")
    void createFloor_duplicateCode_throwsBizException() {
        FloorSaveDTO dto = new FloorSaveDTO();
        dto.setBuildingId(1L);
        dto.setProjectId(1L);
        dto.setFloorCode("F-DUP");
        dto.setFloorName("重复楼层");

        doReturn(1L).when(floorService).count(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> floorService.createFloor(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-U-03 删除-有商铺抛异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-U-03 删除楼层-有商铺关联-抛出BizException含商铺数")
    void deleteFloor_hasShops_throwsBizException() {
        BizFloor existing = new BizFloor();
        existing.setId(1L);
        existing.setIsDeleted(0);

        doReturn(existing).when(floorService).getById(1L);
        when(shopMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> floorService.deleteFloor(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("商铺");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-U-04 删除-无商铺成功
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-U-04 删除楼层-无商铺关联-调用removeById")
    void deleteFloor_noShops_callsRemoveById() {
        BizFloor existing = new BizFloor();
        existing.setId(1L);
        existing.setIsDeleted(0);

        doReturn(existing).when(floorService).getById(1L);
        when(shopMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(floorService).removeById(1L);

        floorService.deleteFloor(1L);

        verify(floorService).removeById(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLR-U-05 新增-编码为空时跳过唯一性校验
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-U-05 新增楼层-编码为空字符串-跳过唯一性校验")
    void createFloor_blankCode_skipCheck() {
        FloorSaveDTO dto = new FloorSaveDTO();
        dto.setBuildingId(1L);
        dto.setProjectId(1L);
        dto.setFloorCode(""); // 空编码
        dto.setFloorName("无编码楼层");

        BizFloor entity = new BizFloor();
        entity.setId(100L);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(true).when(floorService).save(entity);

        floorService.createFloor(dto);

        // 空编码时不应做唯一性校验
        verify(floorService, never()).count(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 枚举名称填充
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLR-U-06 枚举名称填充-status=1返回启用")
    void getFloorById_enumNameFilledCorrectly() {
        BizFloor floor = new BizFloor();
        floor.setId(1L);
        floor.setIsDeleted(0);

        FloorVO vo = new FloorVO();
        vo.setStatus(1);

        doReturn(floor).when(floorService).getById(1L);
        when(converter.toVO(floor)).thenReturn(vo);

        FloorVO result = floorService.getFloorById(1L);

        assertThat(result.getStatusName()).isEqualTo("启用");
    }
}
