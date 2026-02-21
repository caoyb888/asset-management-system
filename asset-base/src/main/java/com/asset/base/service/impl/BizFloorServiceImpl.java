package com.asset.base.service.impl;

import com.asset.base.converter.FloorConverter;
import com.asset.base.entity.BizFloor;
import com.asset.base.mapper.BizFloorMapper;
import com.asset.base.model.dto.FloorQuery;
import com.asset.base.model.dto.FloorSaveDTO;
import com.asset.base.model.vo.FloorVO;
import com.asset.base.service.BizFloorService;
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
 * 楼层管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizFloorServiceImpl
        extends ServiceImpl<BizFloorMapper, BizFloor>
        implements BizFloorService {

    private final FloorConverter converter;

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            0, "停用", 1, "启用");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<FloorVO> pageFloor(FloorQuery query) {
        Page<FloorVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<FloorVO> result = baseMapper.selectPageWithCond(page, query);
        result.getRecords().forEach(this::fillEnumNames);
        return result;
    }

    @Override
    public FloorVO getFloorById(Long id) {
        BizFloor floor = getById(id);
        if (floor == null || floor.getIsDeleted() == 1) {
            throw new BizException("楼层不存在或已删除");
        }
        FloorVO vo = converter.toVO(floor);
        fillEnumNames(vo);
        return vo;
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFloor(FloorSaveDTO dto) {
        checkFloorCodeUnique(dto.getBuildingId(), dto.getFloorCode(), null);
        BizFloor entity = converter.toEntity(dto);
        save(entity);
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFloor(Long id, FloorSaveDTO dto) {
        BizFloor existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("楼层不存在或已删除");
        }
        if (dto.getFloorCode() != null
                && !dto.getFloorCode().equals(existing.getFloorCode())) {
            checkFloorCodeUnique(existing.getBuildingId(), dto.getFloorCode(), id);
        }
        converter.updateEntity(dto, existing);
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFloor(Long id) {
        BizFloor existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("楼层不存在或已删除");
        }
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    /** 同一楼栋内楼层编码唯一性校验 */
    private void checkFloorCodeUnique(Long buildingId, String code, Long excludeId) {
        if (code == null || code.isBlank()) return;
        LambdaQueryWrapper<BizFloor> wrapper = new LambdaQueryWrapper<BizFloor>()
                .eq(BizFloor::getBuildingId, buildingId)
                .eq(BizFloor::getFloorCode, code)
                .eq(BizFloor::getIsDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(BizFloor::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BizException("楼层编码 [" + code + "] 在该楼栋下已存在");
        }
    }

    /** 填充枚举名称 */
    private void fillEnumNames(FloorVO vo) {
        if (vo.getStatus() != null) {
            vo.setStatusName(STATUS_MAP.getOrDefault(vo.getStatus(), "未知"));
        }
    }
}
