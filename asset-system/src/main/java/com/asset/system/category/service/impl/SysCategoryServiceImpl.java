package com.asset.system.category.service.impl;

import com.asset.system.category.dto.CategoryCreateDTO;
import com.asset.system.category.dto.CategoryQueryDTO;
import com.asset.system.category.dto.CategoryTreeVO;
import com.asset.system.category.entity.SysCategory;
import com.asset.system.category.mapper.SysCategoryMapper;
import com.asset.system.category.service.SysCategoryService;
import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 系统分类 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysCategoryServiceImpl extends ServiceImpl<SysCategoryMapper, SysCategory>
        implements SysCategoryService {

    @Override
    public List<CategoryTreeVO> treeQuery(CategoryQueryDTO query) {
        LambdaQueryWrapper<SysCategory> wrapper = new LambdaQueryWrapper<SysCategory>()
                .eq(StringUtils.hasText(query.getCategoryType()), SysCategory::getCategoryType, query.getCategoryType())
                .like(StringUtils.hasText(query.getCategoryName()), SysCategory::getCategoryName, query.getCategoryName())
                .eq(query.getStatus() != null, SysCategory::getStatus, query.getStatus())
                .orderByAsc(SysCategory::getSortOrder, SysCategory::getId);

        List<SysCategory> all = baseMapper.selectList(wrapper);
        return buildTree(all, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryCreateDTO dto) {
        // 同维度下编码唯一校验
        long count = baseMapper.selectCount(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getCategoryType, dto.getCategoryType())
                .eq(SysCategory::getCategoryCode, dto.getCategoryCode()));
        if (count > 0) throw new SysBizException(SysErrorCode.CATEGORY_CODE_EXISTS);

        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;
        String ancestors = "0";
        int level = 1;

        if (parentId != 0L) {
            SysCategory parent = baseMapper.selectById(parentId);
            if (parent == null) throw new SysBizException(SysErrorCode.CATEGORY_NOT_FOUND);
            ancestors = parent.getAncestors() + "," + parentId;
            level = parent.getLevel() + 1;
        }

        SysCategory cat = new SysCategory();
        cat.setCategoryType(dto.getCategoryType());
        cat.setParentId(parentId);
        cat.setAncestors(ancestors);
        cat.setCategoryCode(dto.getCategoryCode());
        cat.setCategoryName(dto.getCategoryName());
        cat.setLevel(level);
        cat.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        cat.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        cat.setRemark(dto.getRemark());
        baseMapper.insert(cat);
        log.info("[分类] 新增分类 type={} code={} name={}", cat.getCategoryType(), cat.getCategoryCode(), cat.getCategoryName());
        return cat.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategoryCreateDTO dto) {
        SysCategory exist = baseMapper.selectById(dto.getId());
        if (exist == null) throw new SysBizException(SysErrorCode.CATEGORY_NOT_FOUND);

        // 编码变更时校验唯一性
        if (StringUtils.hasText(dto.getCategoryCode()) && !dto.getCategoryCode().equals(exist.getCategoryCode())) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<SysCategory>()
                    .eq(SysCategory::getCategoryType, exist.getCategoryType())
                    .eq(SysCategory::getCategoryCode, dto.getCategoryCode())
                    .ne(SysCategory::getId, dto.getId()));
            if (count > 0) throw new SysBizException(SysErrorCode.CATEGORY_CODE_EXISTS);
        }

        update(new LambdaUpdateWrapper<SysCategory>()
                .eq(SysCategory::getId, dto.getId())
                .set(StringUtils.hasText(dto.getCategoryCode()), SysCategory::getCategoryCode, dto.getCategoryCode())
                .set(StringUtils.hasText(dto.getCategoryName()), SysCategory::getCategoryName, dto.getCategoryName())
                .set(dto.getSortOrder() != null, SysCategory::getSortOrder, dto.getSortOrder())
                .set(dto.getStatus() != null, SysCategory::getStatus, dto.getStatus())
                .set(dto.getRemark() != null, SysCategory::getRemark, dto.getRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.CATEGORY_NOT_FOUND);
        if (baseMapper.countChildren(id) > 0) throw new SysBizException(SysErrorCode.CATEGORY_HAS_CHILDREN);
        removeById(id);
        log.info("[分类] 删除分类 id={}", id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.CATEGORY_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysCategory>().eq(SysCategory::getId, id).set(SysCategory::getStatus, status));
    }

    @Override
    public List<String> listCategoryTypes() {
        return baseMapper.selectList(new LambdaQueryWrapper<SysCategory>()
                        .select(SysCategory::getCategoryType)
                        .groupBy(SysCategory::getCategoryType))
                .stream().map(SysCategory::getCategoryType).collect(Collectors.toList());
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private List<CategoryTreeVO> buildTree(List<SysCategory> all, Long rootParentId) {
        Map<Long, List<SysCategory>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(SysCategory::getParentId));
        return buildChildren(childrenMap, rootParentId);
    }

    private List<CategoryTreeVO> buildChildren(Map<Long, List<SysCategory>> map, Long parentId) {
        List<SysCategory> children = map.getOrDefault(parentId, new ArrayList<>());
        return children.stream().map(c -> {
            CategoryTreeVO vo = toVO(c);
            vo.setChildren(buildChildren(map, c.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private CategoryTreeVO toVO(SysCategory c) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setId(c.getId());
        vo.setCategoryType(c.getCategoryType());
        vo.setParentId(c.getParentId());
        vo.setCategoryCode(c.getCategoryCode());
        vo.setCategoryName(c.getCategoryName());
        vo.setLevel(c.getLevel());
        vo.setSortOrder(c.getSortOrder());
        vo.setStatus(c.getStatus());
        vo.setRemark(c.getRemark());
        return vo;
    }
}
