package com.asset.base.service;

import com.asset.base.converter.BrandConverter;
import com.asset.base.entity.BizBrand;
import com.asset.base.entity.BizBrandContact;
import com.asset.base.mapper.BizBrandContactMapper;
import com.asset.base.mapper.BizBrandMapper;
import com.asset.base.model.dto.BrandContactDTO;
import com.asset.base.model.dto.BrandSaveDTO;
import com.asset.base.model.vo.BrandContactVO;
import com.asset.base.model.vo.BrandVO;
import com.asset.base.service.impl.BizBrandServiceImpl;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 品牌管理 Service 单元测试（BRAND-U-01 ~ BRAND-U-08）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("品牌管理 Service 单元测试")
class BizBrandServiceTest {

    @Mock
    BizBrandMapper brandMapper;

    @Mock
    BrandConverter converter;

    @Mock
    BizBrandContactMapper contactMapper;

    @Spy
    @InjectMocks
    BizBrandServiceImpl brandService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(brandService, "baseMapper", brandMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-01 新增品牌-含联系人
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-01 新增品牌-含2个联系人-contactMapper.insert调用2次")
    void createBrand_withContacts_savesAll() {
        BrandSaveDTO dto = new BrandSaveDTO();
        dto.setBrandNameCn("测试品牌");
        BrandContactDTO c1 = new BrandContactDTO();
        c1.setContactName("联系人A");
        BrandContactDTO c2 = new BrandContactDTO();
        c2.setContactName("联系人B");
        dto.setContacts(List.of(c1, c2));

        BizBrand entity = new BizBrand();
        entity.setId(100L);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(true).when(brandService).save(entity);
        when(converter.toContactEntity(any(BrandContactDTO.class)))
                .thenAnswer(inv -> new BizBrandContact());
        when(contactMapper.insert(any(BizBrandContact.class))).thenReturn(1);

        brandService.createBrand(dto);

        verify(contactMapper, times(2)).insert(any(BizBrandContact.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-02 新增品牌-无联系人
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-02 新增品牌-无联系人-contactMapper.insert不调用")
    void createBrand_noContacts_skipInsert() {
        BrandSaveDTO dto = new BrandSaveDTO();
        dto.setBrandNameCn("无联系人品牌");
        dto.setContacts(List.of()); // 空列表

        BizBrand entity = new BizBrand();
        entity.setId(101L);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(true).when(brandService).save(entity);

        brandService.createBrand(dto);

        verify(contactMapper, never()).insert(any(BizBrandContact.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-03 编辑品牌-重建联系人
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-03 编辑品牌-重建联系人-先delete再insert 2次")
    void updateBrand_rebuildsContacts() {
        BizBrand existing = new BizBrand();
        existing.setId(1L);
        existing.setIsDeleted(0);

        BrandSaveDTO dto = new BrandSaveDTO();
        dto.setBrandNameCn("更新品牌");
        BrandContactDTO c1 = new BrandContactDTO();
        c1.setContactName("新联系人A");
        BrandContactDTO c2 = new BrandContactDTO();
        c2.setContactName("新联系人B");
        dto.setContacts(List.of(c1, c2));

        doReturn(existing).when(brandService).getById(1L);
        doReturn(true).when(brandService).updateById(any());
        when(contactMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(converter.toContactEntity(any(BrandContactDTO.class)))
                .thenAnswer(inv -> new BizBrandContact());
        when(contactMapper.insert(any(BizBrandContact.class))).thenReturn(1);

        brandService.updateBrand(1L, dto);

        verify(contactMapper).delete(any(LambdaQueryWrapper.class));
        verify(contactMapper, times(2)).insert(any(BizBrandContact.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-04 编辑-不存在品牌
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-04 编辑品牌-ID不存在-抛出BizException")
    void updateBrand_notFound_throws() {
        doReturn(null).when(brandService).getById(999999L);

        BrandSaveDTO dto = new BrandSaveDTO();
        dto.setBrandNameCn("不存在品牌");

        assertThatThrownBy(() -> brandService.updateBrand(999999L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-05 删除-不存在品牌
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-05 删除品牌-ID不存在-抛出BizException")
    void deleteBrand_notFound_throws() {
        doReturn(null).when(brandService).getById(999999L);

        assertThatThrownBy(() -> brandService.deleteBrand(999999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-06 详情-含联系人列表，主要联系人排前
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-06 品牌详情-含2个联系人-返回VO中contacts长度=2")
    void getBrandById_includesContacts() {
        BizBrand brand = new BizBrand();
        brand.setId(1L);
        brand.setIsDeleted(0);

        BrandVO vo = new BrandVO();
        vo.setId(1L);

        BizBrandContact pc1 = new BizBrandContact();
        pc1.setIsPrimary(1);
        BizBrandContact pc2 = new BizBrandContact();
        pc2.setIsPrimary(0);

        doReturn(brand).when(brandService).getById(1L);
        when(converter.toVO(brand)).thenReturn(vo);
        when(contactMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(pc1, pc2));
        when(converter.toContactVO(pc1)).thenReturn(new BrandContactVO());
        when(converter.toContactVO(pc2)).thenReturn(new BrandContactVO());

        BrandVO result = brandService.getBrandById(1L);

        assertThat(result.getContacts()).hasSize(2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-07 枚举名称填充-全字段
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-07 枚举名称填充-全字段-返回正确中文名称")
    void getBrandById_enumNamesFilledCorrectly() {
        BizBrand brand = new BizBrand();
        brand.setId(1L);
        brand.setIsDeleted(0);

        BrandVO vo = new BrandVO();
        vo.setBrandLevel(1);       // 高端
        vo.setCooperationType(2);  // 加盟
        vo.setBusinessNature(3);   // 娱乐
        vo.setChainType(1);        // 连锁
        vo.setBrandType(2);        // 商街

        doReturn(brand).when(brandService).getById(1L);
        when(converter.toVO(brand)).thenReturn(vo);
        when(contactMapper.selectList(any())).thenReturn(List.of());

        BrandVO result = brandService.getBrandById(1L);

        assertThat(result.getBrandLevelName()).isEqualTo("高端");
        assertThat(result.getCooperationTypeName()).isEqualTo("加盟");
        assertThat(result.getBusinessNatureName()).isEqualTo("娱乐");
        assertThat(result.getChainTypeName()).isEqualTo("连锁");
        assertThat(result.getBrandTypeName()).isEqualTo("商街");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND-U-08 枚举名称-未知值返回"未知"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BRAND-U-08 枚举名称-未知枚举值-返回未知")
    void getBrandById_unknownEnumValue_returnsUnknown() {
        BizBrand brand = new BizBrand();
        brand.setId(1L);
        brand.setIsDeleted(0);

        BrandVO vo = new BrandVO();
        vo.setBrandLevel(99); // 不存在的枚举值

        doReturn(brand).when(brandService).getById(1L);
        when(converter.toVO(brand)).thenReturn(vo);
        when(contactMapper.selectList(any())).thenReturn(List.of());

        BrandVO result = brandService.getBrandById(1L);

        assertThat(result.getBrandLevelName()).isEqualTo("未知");
    }
}
