package com.asset.base.service.impl;

import com.asset.base.converter.BrandConverter;
import com.asset.base.entity.BizBrand;
import com.asset.base.entity.BizBrandContact;
import com.asset.base.mapper.BizBrandContactMapper;
import com.asset.base.mapper.BizBrandMapper;
import com.asset.base.model.dto.BrandContactDTO;
import com.asset.base.model.dto.BrandQuery;
import com.asset.base.model.dto.BrandSaveDTO;
import com.asset.base.model.vo.BrandVO;
import com.asset.base.service.BizBrandService;
import com.asset.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 品牌管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizBrandServiceImpl
        extends ServiceImpl<BizBrandMapper, BizBrand>
        implements BizBrandService {

    private final BrandConverter converter;
    private final BizBrandContactMapper contactMapper;

    private static final Map<Integer, String> BRAND_LEVEL_MAP = Map.of(
            1, "高端", 2, "中端", 3, "大众");

    private static final Map<Integer, String> COOPERATION_TYPE_MAP = Map.of(
            1, "直营", 2, "加盟", 3, "代理");

    private static final Map<Integer, String> BUSINESS_NATURE_MAP = Map.of(
            1, "餐饮", 2, "零售", 3, "娱乐", 4, "服务");

    private static final Map<Integer, String> CHAIN_TYPE_MAP = Map.of(
            1, "连锁", 2, "单店");

    private static final Map<Integer, String> BRAND_TYPE_MAP = Map.of(
            1, "MALL", 2, "商街");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<BrandVO> pageBrand(BrandQuery query) {
        Page<BrandVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<BrandVO> result = baseMapper.selectPageWithCond(page, query);
        result.getRecords().forEach(this::fillEnumNames);
        return result;
    }

    @Override
    public BrandVO getBrandById(Long id) {
        BizBrand brand = getById(id);
        if (brand == null || brand.getIsDeleted() == 1) {
            throw new BizException("品牌不存在或已删除");
        }
        BrandVO vo = converter.toVO(brand);
        fillEnumNames(vo);
        // 查询联系人列表
        List<BizBrandContact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<BizBrandContact>()
                        .eq(BizBrandContact::getBrandId, id)
                        .eq(BizBrandContact::getIsDeleted, 0)
                        .orderByDesc(BizBrandContact::getIsPrimary)
        );
        vo.setContacts(contacts.stream().map(converter::toContactVO).toList());
        return vo;
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBrand(BrandSaveDTO dto) {
        BizBrand entity = converter.toEntity(dto);
        save(entity);
        saveContacts(entity.getId(), dto.getContacts());
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBrand(Long id, BrandSaveDTO dto) {
        BizBrand existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("品牌不存在或已删除");
        }
        converter.updateEntity(dto, existing);
        updateById(existing);
        // 重建联系人：先逻辑删除旧的，再插入新的
        contactMapper.delete(
                new LambdaQueryWrapper<BizBrandContact>()
                        .eq(BizBrandContact::getBrandId, id)
        );
        saveContacts(id, dto.getContacts());
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBrand(Long id) {
        BizBrand existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("品牌不存在或已删除");
        }
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    /** 批量插入联系人 */
    private void saveContacts(Long brandId, List<BrandContactDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return;
        }
        dtos.forEach(dto -> {
            BizBrandContact contact = converter.toContactEntity(dto);
            contact.setId(null); // 重建联系人时清除旧id，由雪花算法重新生成
            contact.setBrandId(brandId);
            contactMapper.insert(contact);
        });
    }

    /** 填充枚举名称 */
    private void fillEnumNames(BrandVO vo) {
        if (vo.getBrandLevel() != null) {
            vo.setBrandLevelName(BRAND_LEVEL_MAP.getOrDefault(vo.getBrandLevel(), "未知"));
        }
        if (vo.getCooperationType() != null) {
            vo.setCooperationTypeName(COOPERATION_TYPE_MAP.getOrDefault(vo.getCooperationType(), "未知"));
        }
        if (vo.getBusinessNature() != null) {
            vo.setBusinessNatureName(BUSINESS_NATURE_MAP.getOrDefault(vo.getBusinessNature(), "未知"));
        }
        if (vo.getChainType() != null) {
            vo.setChainTypeName(CHAIN_TYPE_MAP.getOrDefault(vo.getChainType(), "未知"));
        }
        if (vo.getBrandType() != null) {
            vo.setBrandTypeName(BRAND_TYPE_MAP.getOrDefault(vo.getBrandType(), "未知"));
        }
    }
}
