package com.asset.report.favorite;

import com.asset.common.model.R;
import com.asset.report.favorite.dto.FavoriteAddDTO;
import com.asset.report.favorite.dto.FavoriteSortDTO;
import com.asset.report.favorite.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表收藏接口
 * <p>
 * 路径前缀：/rpt/common/favorites
 * </p>
 */
@Tag(name = "报表收藏", description = "用户报表收藏与快捷入口管理")
@RestController
@RequestMapping("/rpt/common/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 查询当前用户的收藏列表
     */
    @Operation(summary = "我的收藏列表", description = "返回当前登录用户的全部报表收藏，按 sort_order 升序")
    @GetMapping
    public R<List<FavoriteVO>> list() {
        return R.ok(favoriteService.listMyFavorites());
    }

    /**
     * 新增收藏
     * <p>同一报表已收藏时直接返回已有记录 ID，不重复插入。</p>
     */
    @Operation(summary = "收藏报表", description = "收藏指定报表，重复收藏返回已有 ID")
    @PostMapping
    public R<Long> add(@Valid @RequestBody FavoriteAddDTO dto) {
        return R.ok(favoriteService.addFavorite(dto));
    }

    /**
     * 取消收藏
     */
    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}")
    public R<Void> remove(
            @Parameter(description = "收藏记录 ID") @PathVariable Long id) {
        favoriteService.removeFavorite(id);
        return R.ok();
    }

    /**
     * 拖拽排序：前端拖拽完成后提交新顺序
     */
    @Operation(summary = "更新排序", description = "按 ids 列表顺序重置 sort_order，支持拖拽排序")
    @PutMapping("/sort")
    public R<Void> sort(@Valid @RequestBody FavoriteSortDTO dto) {
        favoriteService.updateSort(dto);
        return R.ok();
    }
}
