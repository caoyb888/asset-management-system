package com.asset.base.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商铺新增/编辑 DTO
 */
@Data
public class ShopSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 所属项目ID */
    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    /** 所属楼栋ID */
    @NotNull(message = "所属楼栋不能为空")
    private Long buildingId;

    /** 所在楼层ID */
    @NotNull(message = "所在楼层不能为空")
    private Long floorId;

    /** 铺位号 */
    @NotBlank(message = "铺位号不能为空")
    private String shopCode;

    /** 商铺类型：1临街 2内铺 3专柜 */
    private Integer shopType;

    /** 计租面积(㎡) */
    @DecimalMin(value = "0", message = "计租面积不能为负数")
    private BigDecimal rentArea;

    /** 实测面积(㎡) */
    @DecimalMin(value = "0", message = "实测面积不能为负数")
    private BigDecimal measuredArea;

    /** 建筑面积(㎡) */
    @DecimalMin(value = "0", message = "建筑面积不能为负数")
    private BigDecimal buildingArea;

    /** 经营面积(㎡) */
    @DecimalMin(value = "0", message = "经营面积不能为负数")
    private BigDecimal operatingArea;

    /** 商铺状态：0空置 1在租 2自用 3预留 */
    private Integer shopStatus;

    /** 计入招商率：0否 1是 */
    private Integer countLeasingRate;

    /** 计入出租率：0否 1是 */
    private Integer countRentalRate;

    /** 计入开业率：0否 1是 */
    private Integer countOpeningRate;

    /** 签约业态 */
    private String signedFormat;

    /** 规划业态 */
    private String plannedFormat;

    /** 业主名称 */
    private String ownerName;

    /** 业主联系人 */
    private String ownerContact;

    /** 业主电话 */
    private String ownerPhone;
}
