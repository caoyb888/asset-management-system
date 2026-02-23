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
 * 商铺拆分 DTO
 */
@Data
public class ShopSplitDTO {

    /** 源商铺ID */
    @NotNull(message = "源商铺ID不能为空")
    private Long sourceShopId;

    /** 备注 */
    private String remark;

    /** 拆分后商铺列表（至少2个） */
    @Valid
    @NotEmpty(message = "拆分后商铺列表不能为空")
    @Size(min = 2, message = "至少拆分为2个商铺")
    private List<SubShopDTO> subShops;

    /**
     * 子商铺信息
     */
    @Data
    public static class SubShopDTO {

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
