package com.asset.report.favorite;

import com.asset.report.favorite.dto.FavoriteAddDTO;
import com.asset.report.favorite.dto.FavoriteSortDTO;
import com.asset.report.favorite.vo.FavoriteVO;

import java.util.List;

/**
 * 报表收藏 Service
 */
public interface FavoriteService {

    /**
     * 查询当前用户的收藏列表（按排序升序）
     */
    List<FavoriteVO> listMyFavorites();

    /**
     * 收藏报表（同一报表重复收藏返回已有 ID）
     *
     * @return 收藏记录 ID
     */
    Long addFavorite(FavoriteAddDTO dto);

    /**
     * 取消收藏
     *
     * @param id 收藏记录 ID
     */
    void removeFavorite(Long id);

    /**
     * 拖拽排序：按 ids 列表顺序重置 sort_order
     */
    void updateSort(FavoriteSortDTO dto);
}
