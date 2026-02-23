package com.asset.base.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商铺合并 DTO
 */
@Data
public class ShopMergeDTO {

    /** 需合并的源商铺ID列表（至少2个） */
    @NotEmpty(message = "合并商铺ID列表不能为空")
    @Size(min = 2, message = "至少选择2个商铺合并")
    private List<Long> sourceShopIds;

    /** 备注 */
    private String remark;

    /** 合并后的新商铺信息 */
    @Valid
    @NotNull(message = "合并后商铺信息不能为空")
    private MergedShopDTO newShop;

    /**
     * 合并后新商铺信息
     */
    @Data
    public static class MergedShopDTO {

        /** 铺位号 */
        @NotBlank(message = "铺位号不能为空")
        private String shopCode;

        /** 商铺类型：1临街 2内铺 3专柜 */
        private Integer shopType;

        /** 计租面积(㎡) */
        @NotNull(message = "计租面积不能为空")
        private BigDecimal rentArea;

        /** 实测面积(㎡) */
        private BigDecimal measuredArea;

        /** 建筑面积(㎡) */
        private BigDecimal buildingArea;

        /** 经营面积(㎡) */
        private BigDecimal operatingArea;

        /** 规划业态 */
        private String plannedFormat;
    }
}
