package com.asset.base.service.impl;

import com.asset.base.entity.BizNews;
import com.asset.base.mapper.BizNewsMapper;
import com.asset.base.model.dto.NewsQuery;
import com.asset.base.model.dto.NewsSaveDTO;
import com.asset.base.model.vo.NewsVO;
import com.asset.base.service.BizNewsService;
import com.asset.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 新闻资讯 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizNewsServiceImpl
        extends ServiceImpl<BizNewsMapper, BizNews>
        implements BizNewsService {

    private static final Map<Integer, String> CATEGORY_MAP = Map.of(
            1, "新闻", 2, "政策", 3, "招商动态", 4, "服务指南");

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            0, "草稿", 1, "上架", 2, "下架");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<NewsVO> pageNews(NewsQuery query) {
        LambdaQueryWrapper<BizNews> wrapper = new LambdaQueryWrapper<BizNews>()
                .eq(BizNews::getIsDeleted, 0)
                .like(StringUtils.hasText(query.getTitle()), BizNews::getTitle, query.getTitle())
                .eq(query.getCategory() != null, BizNews::getCategory, query.getCategory())
                .eq(query.getStatus() != null, BizNews::getStatus, query.getStatus())
                .orderByDesc(BizNews::getCreatedAt);
        Page<BizNews> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<BizNews> entityPage = page(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public NewsVO getNewsById(Long id) {
        BizNews news = getById(id);
        if (news == null || news.getIsDeleted() == 1) {
            throw new BizException("新闻资讯不存在或已删除");
        }
        return toVO(news);
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNews(NewsSaveDTO dto) {
        BizNews entity = new BizNews();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        // 若保存时直接设为上架状态，记录发布时间
        if (Integer.valueOf(1).equals(entity.getStatus()) && entity.getPublishTime() == null) {
            entity.setPublishTime(LocalDateTime.now());
        }
        save(entity);
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNews(Long id, NewsSaveDTO dto) {
        BizNews existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("新闻资讯不存在或已删除");
        }
        BeanUtils.copyProperties(dto, existing, "id");
        // 若状态变更为上架且原先没有发布时间，记录发布时间
        if (Integer.valueOf(1).equals(existing.getStatus()) && existing.getPublishTime() == null) {
            existing.setPublishTime(LocalDateTime.now());
        }
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNews(Long id) {
        BizNews existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("新闻资讯不存在或已删除");
        }
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 上架 / 下架                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNews(Long id) {
        BizNews existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("新闻资讯不存在或已删除");
        }
        existing.setStatus(1);
        existing.setPublishTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishNews(Long id) {
        BizNews existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("新闻资讯不存在或已删除");
        }
        existing.setStatus(2);
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    /** Entity → VO */
    private NewsVO toVO(BizNews entity) {
        NewsVO vo = new NewsVO();
        BeanUtils.copyProperties(entity, vo);
        fillEnumNames(vo);
        return vo;
    }

    /** 填充枚举名称 */
    private void fillEnumNames(NewsVO vo) {
        if (vo.getCategory() != null) {
            vo.setCategoryName(CATEGORY_MAP.getOrDefault(vo.getCategory(), "未知"));
        }
        if (vo.getStatus() != null) {
            vo.setStatusName(STATUS_MAP.getOrDefault(vo.getStatus(), "未知"));
        }
    }
}
