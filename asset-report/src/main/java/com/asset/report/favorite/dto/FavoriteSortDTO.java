package com.asset.report.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 拖拽排序请求 DTO
 */
@Data
@Schema(description = "收藏排序更新")
public class FavoriteSortDTO {

    @Schema(description = "按新顺序排列的收藏 ID 列表")
    @NotEmpty(message = "ids 不能为空")
    private List<Long> ids;
}
