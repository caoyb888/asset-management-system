package com.asset.base.service.impl;

import com.asset.base.converter.ShopConverter;
import com.asset.base.entity.BizShop;
import com.asset.base.mapper.BizShopMapper;
import com.asset.base.model.dto.ShopQuery;
import com.asset.base.model.dto.ShopSaveDTO;
import com.asset.base.model.vo.ShopVO;
import com.asset.base.service.BizShopService;
import com.asset.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 商铺管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizShopServiceImpl
        extends ServiceImpl<BizShopMapper, BizShop>
        implements BizShopService {

    private final ShopConverter converter;

    private static final Map<Integer, String> SHOP_TYPE_MAP = Map.of(
            1, "临街", 2, "内铺", 3, "专柜");

    private static final Map<Integer, String> SHOP_STATUS_MAP = Map.of(
            0, "空置", 1, "在租", 2, "自用", 3, "预留");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<ShopVO> pageShop(ShopQuery query) {
        Page<ShopVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<ShopVO> result = baseMapper.selectPageWithCond(page, query);
        result.getRecords().forEach(this::fillEnumNames);
        return result;
    }

    @Override
    public ShopVO getShopById(Long id) {
        BizShop shop = getById(id);
        if (shop == null || shop.getIsDeleted() == 1) {
            throw new BizException("商铺不存在或已删除");
        }
        ShopVO vo = converter.toVO(shop);
        fillEnumNames(vo);
        return vo;
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShop(ShopSaveDTO dto) {
        checkShopCodeUnique(dto.getProjectId(), dto.getShopCode(), null);
        BizShop entity = converter.toEntity(dto);
        save(entity);
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShop(Long id, ShopSaveDTO dto) {
        BizShop existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("商铺不存在或已删除");
        }
        if (!existing.getShopCode().equals(dto.getShopCode())) {
            checkShopCodeUnique(existing.getProjectId(), dto.getShopCode(), id);
        }
        converter.updateEntity(dto, existing);
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShop(Long id) {
        BizShop existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("商铺不存在或已删除");
        }
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    /** 同一项目内铺位号唯一性校验 */
    private void checkShopCodeUnique(Long projectId, String code, Long excludeId) {
        LambdaQueryWrapper<BizShop> wrapper = new LambdaQueryWrapper<BizShop>()
                .eq(BizShop::getProjectId, projectId)
                .eq(BizShop::getShopCode, code)
                .eq(BizShop::getIsDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(BizShop::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BizException("铺位号 [" + code + "] 在该项目下已存在");
        }
    }

    /** 填充枚举名称 */
    private void fillEnumNames(ShopVO vo) {
        if (vo.getShopType() != null) {
            vo.setShopTypeName(SHOP_TYPE_MAP.getOrDefault(vo.getShopType(), "未知"));
        }
        if (vo.getShopStatus() != null) {
            vo.setShopStatusName(SHOP_STATUS_MAP.getOrDefault(vo.getShopStatus(), "未知"));
        }
    }
}
