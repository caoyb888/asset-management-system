package com.asset.report.favorite;

import com.asset.common.security.util.SecurityUtil;
import com.asset.report.entity.RptUserFavorite;
import com.asset.report.favorite.dto.FavoriteAddDTO;
import com.asset.report.favorite.dto.FavoriteSortDTO;
import com.asset.report.favorite.vo.FavoriteVO;
import com.asset.report.mapper.rpt.RptUserFavoriteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 报表收藏 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final RptUserFavoriteMapper favoriteMapper;

    @Override
    public List<FavoriteVO> listMyFavorites() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == 0L) {
            return List.of();
        }
        List<RptUserFavorite> list = favoriteMapper.selectByUserId(userId);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFavorite(FavoriteAddDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == 0L) {
            throw new IllegalStateException("用户未登录");
        }

        // 检查是否已收藏
        int exists = favoriteMapper.countByUserAndCode(userId, dto.getReportCode());
        if (exists > 0) {
            // 已收藏，返回已有记录 ID
            List<RptUserFavorite> list = favoriteMapper.selectByUserId(userId);
            return list.stream()
                    .filter(f -> dto.getReportCode().equals(f.getReportCode()))
                    .findFirst()
                    .map(RptUserFavorite::getId)
                    .orElse(0L);
        }

        // 计算排序：放到最后
        List<RptUserFavorite> existing = favoriteMapper.selectByUserId(userId);
        int nextSort = existing.isEmpty() ? 0 : existing.size();

        RptUserFavorite entity = new RptUserFavorite()
                .setUserId(userId)
                .setReportId(0L)
                .setReportCode(dto.getReportCode())
                .setReportName(dto.getReportName())
                .setRoutePath(dto.getRoutePath())
                .setCategory(dto.getCategory())
                .setSortOrder(nextSort)
                .setQuickAccess(Boolean.TRUE.equals(dto.getQuickAccess()));

        favoriteMapper.insert(entity);
        log.info("[Favorite] 用户 {} 收藏报表: {}", userId, dto.getReportCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == 0L) {
            throw new IllegalStateException("用户未登录");
        }

        // 校验归属（只能删除自己的收藏）
        RptUserFavorite entity = favoriteMapper.selectById(id);
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("收藏记录不存在或无权操作");
        }

        favoriteMapper.update(null, new LambdaUpdateWrapper<RptUserFavorite>()
                .eq(RptUserFavorite::getId, id)
                .eq(RptUserFavorite::getUserId, userId)
                .set(RptUserFavorite::getIsDeleted, true));
        log.info("[Favorite] 用户 {} 取消收藏: id={}", userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(FavoriteSortDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == 0L) {
            throw new IllegalStateException("用户未登录");
        }

        List<Long> ids = dto.getIds();
        for (int i = 0; i < ids.size(); i++) {
            favoriteMapper.updateSortOrder(ids.get(i), userId, i);
        }
        log.info("[Favorite] 用户 {} 更新排序，共 {} 条", userId, ids.size());
    }

    // ──── 转换 ────

    private FavoriteVO toVO(RptUserFavorite entity) {
        return new FavoriteVO()
                .setId(entity.getId())
                .setReportCode(entity.getReportCode())
                .setReportName(entity.getReportName())
                .setRoutePath(entity.getRoutePath())
                .setCategory(entity.getCategory())
                .setSortOrder(entity.getSortOrder())
                .setQuickAccess(entity.getQuickAccess())
                .setCreatedAt(entity.getCreatedAt());
    }
}
