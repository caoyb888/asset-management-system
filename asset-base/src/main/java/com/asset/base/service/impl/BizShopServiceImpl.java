package com.asset.base.service.impl;

import com.asset.base.converter.ShopConverter;
import com.asset.base.entity.BizShop;
import com.asset.base.entity.BizShopRelation;
import com.asset.base.mapper.BizShopMapper;
import com.asset.base.mapper.BizShopRelationMapper;
import com.asset.base.model.dto.ShopMergeDTO;
import com.asset.base.model.dto.ShopQuery;
import com.asset.base.model.dto.ShopSaveDTO;
import com.asset.base.model.dto.ShopSplitDTO;
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

import java.math.BigDecimal;
import java.util.List;
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
    private final BizShopRelationMapper shopRelationMapper;

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
    /* 拆分                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void splitShop(ShopSplitDTO dto) {
        // 1. 查询源商铺（校验存在 + is_deleted=0）
        BizShop source = getById(dto.getSourceShopId());
        if (source == null || source.getIsDeleted() == 1) {
            throw new BizException("源商铺不存在或已删除");
        }

        // 2. 面积守恒校验：各子商铺计租面积之和与源商铺计租面积差值 <= 0.01
        if (source.getRentArea() != null) {
            BigDecimal subTotal = dto.getSubShops().stream()
                    .map(ShopSplitDTO.SubShopDTO::getRentArea)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (subTotal.subtract(source.getRentArea()).abs()
                    .compareTo(new BigDecimal("0.01")) > 0) {
                throw new BizException("拆分后各商铺计租面积之和与源商铺面积不一致，请检查面积数据");
            }
        }

        // 3. 校验铺位号唯一性（项目内）
        for (ShopSplitDTO.SubShopDTO sub : dto.getSubShops()) {
            checkShopCodeUnique(source.getProjectId(), sub.getShopCode(), null);
        }

        // 4. 源商铺逻辑删除
        removeById(dto.getSourceShopId());

        // 5. 创建各子商铺并写关联记录
        for (ShopSplitDTO.SubShopDTO sub : dto.getSubShops()) {
            BizShop newShop = new BizShop();
            newShop.setProjectId(source.getProjectId());
            newShop.setBuildingId(source.getBuildingId());
            newShop.setFloorId(source.getFloorId());
            newShop.setShopCode(sub.getShopCode());
            newShop.setShopType(sub.getShopType());
            newShop.setRentArea(sub.getRentArea());
            newShop.setMeasuredArea(sub.getMeasuredArea());
            newShop.setBuildingArea(sub.getBuildingArea());
            newShop.setOperatingArea(sub.getOperatingArea());
            newShop.setPlannedFormat(sub.getPlannedFormat());
            newShop.setShopStatus(0);
            save(newShop);

            // 6. 写 biz_shop_relation
            BizShopRelation relation = new BizShopRelation();
            relation.setSourceShopId(source.getId());
            relation.setTargetShopId(newShop.getId());
            relation.setRelationType(1);
            relation.setAreaBefore(source.getRentArea());
            relation.setAreaAfter(sub.getRentArea());
            relation.setRemark(dto.getRemark());
            shopRelationMapper.insert(relation);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 合并                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeShop(ShopMergeDTO dto) {
        // 1. 批量查询源商铺（校验都存在 + is_deleted=0）
        List<BizShop> sources = listByIds(dto.getSourceShopIds());
        if (sources.size() != dto.getSourceShopIds().size()) {
            throw new BizException("部分源商铺不存在或已删除");
        }
        for (BizShop s : sources) {
            if (s.getIsDeleted() == 1) {
                throw new BizException("商铺 [" + s.getShopCode() + "] 已删除，无法参与合并");
            }
        }

        // 2. 校验所有源商铺属于同一楼层
        long distinctFloors = sources.stream()
                .map(BizShop::getFloorId)
                .distinct()
                .count();
        if (distinctFloors > 1) {
            throw new BizException("合并商铺必须属于同一楼层");
        }

        // 3. 面积守恒：新商铺面积与所有源商铺面积之和差值 <= 0.01
        BigDecimal sourceTotal = sources.stream()
                .map(s -> s.getRentArea() != null ? s.getRentArea() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dto.getNewShop().getRentArea().subtract(sourceTotal).abs()
                .compareTo(new BigDecimal("0.01")) > 0) {
            throw new BizException("合并后商铺计租面积与各源商铺面积之和不一致，请检查面积数据");
        }

        // 4. 校验新铺位号唯一性
        BizShop firstSource = sources.get(0);
        checkShopCodeUnique(firstSource.getProjectId(), dto.getNewShop().getShopCode(), null);

        // 5. 源商铺全部逻辑删除
        removeByIds(dto.getSourceShopIds());

        // 6. 创建新商铺
        ShopMergeDTO.MergedShopDTO merged = dto.getNewShop();
        BizShop newShop = new BizShop();
        newShop.setProjectId(firstSource.getProjectId());
        newShop.setBuildingId(firstSource.getBuildingId());
        newShop.setFloorId(firstSource.getFloorId());
        newShop.setShopCode(merged.getShopCode());
        newShop.setShopType(merged.getShopType());
        newShop.setRentArea(merged.getRentArea());
        newShop.setMeasuredArea(merged.getMeasuredArea());
        newShop.setBuildingArea(merged.getBuildingArea());
        newShop.setOperatingArea(merged.getOperatingArea());
        newShop.setPlannedFormat(merged.getPlannedFormat());
        newShop.setShopStatus(0);
        save(newShop);

        // 7. 每个源商铺写 biz_shop_relation
        for (BizShop source : sources) {
            BizShopRelation relation = new BizShopRelation();
            relation.setSourceShopId(source.getId());
            relation.setTargetShopId(newShop.getId());
            relation.setRelationType(2);
            relation.setAreaBefore(source.getRentArea());
            relation.setAreaAfter(merged.getRentArea());
            relation.setRemark(dto.getRemark());
            shopRelationMapper.insert(relation);
        }
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
