package com.asset.base.service.impl;

import com.asset.base.converter.BuildingConverter;
import com.asset.base.entity.BizBuilding;
import com.asset.base.mapper.BizBuildingMapper;
import com.asset.base.model.dto.BuildingQuery;
import com.asset.base.model.dto.BuildingSaveDTO;
import com.asset.base.model.vo.BuildingVO;
import com.asset.base.service.BizBuildingService;
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
 * 楼栋管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizBuildingServiceImpl
        extends ServiceImpl<BizBuildingMapper, BizBuilding>
        implements BizBuildingService {

    private final BuildingConverter converter;

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            0, "停用", 1, "启用");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<BuildingVO> pageBuilding(BuildingQuery query) {
        Page<BuildingVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<BuildingVO> result = baseMapper.selectPageWithCond(page, query);
        result.getRecords().forEach(this::fillEnumNames);
        return result;
    }

    @Override
    public BuildingVO getBuildingById(Long id) {
        BizBuilding building = getById(id);
        if (building == null || building.getIsDeleted() == 1) {
            throw new BizException("楼栋不存在或已删除");
        }
        BuildingVO vo = converter.toVO(building);
        fillEnumNames(vo);
        return vo;
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBuilding(BuildingSaveDTO dto) {
        checkBuildingCodeUnique(dto.getProjectId(), dto.getBuildingCode(), null);
        BizBuilding entity = converter.toEntity(dto);
        save(entity);
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBuilding(Long id, BuildingSaveDTO dto) {
        BizBuilding existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("楼栋不存在或已删除");
        }
        // 编码变更时校验唯一性
        if (dto.getBuildingCode() != null
                && !dto.getBuildingCode().equals(existing.getBuildingCode())) {
            checkBuildingCodeUnique(existing.getProjectId(), dto.getBuildingCode(), id);
        }
        converter.updateEntity(dto, existing);
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBuilding(Long id) {
        BizBuilding existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("楼栋不存在或已删除");
        }
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    /** 同一项目内楼栋编码唯一性校验 */
    private void checkBuildingCodeUnique(Long projectId, String code, Long excludeId) {
        if (code == null || code.isBlank()) return;
        LambdaQueryWrapper<BizBuilding> wrapper = new LambdaQueryWrapper<BizBuilding>()
                .eq(BizBuilding::getProjectId, projectId)
                .eq(BizBuilding::getBuildingCode, code)
                .eq(BizBuilding::getIsDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(BizBuilding::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BizException("楼栋编码 [" + code + "] 在该项目下已存在");
        }
    }

    /** 填充枚举名称 */
    private void fillEnumNames(BuildingVO vo) {
        if (vo.getStatus() != null) {
            vo.setStatusName(STATUS_MAP.getOrDefault(vo.getStatus(), "未知"));
        }
    }
}
