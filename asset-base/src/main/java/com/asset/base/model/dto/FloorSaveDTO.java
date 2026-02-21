package com.asset.base.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 楼层新增/编辑 DTO
 */
@Data
public class FloorSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 所属项目ID */
    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    /** 所属楼栋ID */
    @NotNull(message = "所属楼栋不能为空")
    private Long buildingId;

    /** 楼层编码 */
    private String floorCode;

    /** 楼层名称 */
    @NotBlank(message = "楼层名称不能为空")
    private String floorName;

    /** 状态：0停用 1启用 */
    private Integer status;

    /** 建筑面积(㎡) */
    @DecimalMin(value = "0", message = "建筑面积不能为负数")
    private BigDecimal buildingArea;

    /** 经营面积(㎡) */
    @DecimalMin(value = "0", message = "经营面积不能为负数")
    private BigDecimal operatingArea;

    /** 备注 */
    private String remark;

    /** 楼层平面图URL */
    private String imageUrl;
}
