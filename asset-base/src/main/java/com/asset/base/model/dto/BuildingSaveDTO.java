package com.asset.base.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 楼栋新增/编辑 DTO
 */
@Data
public class BuildingSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 所属项目ID */
    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    /** 楼栋编码 */
    private String buildingCode;

    /** 楼栋名称 */
    @NotBlank(message = "楼栋名称不能为空")
    private String buildingName;

    /** 状态：0停用 1启用 */
    private Integer status;

    /** 建筑面积(㎡) */
    @DecimalMin(value = "0", message = "建筑面积不能为负数")
    private BigDecimal buildingArea;

    /** 营业面积(㎡) */
    @DecimalMin(value = "0", message = "营业面积不能为负数")
    private BigDecimal operatingArea;

    /** 地上楼层数 */
    private Integer aboveFloors;

    /** 地下楼层数 */
    private Integer belowFloors;

    /** 楼栋平面图URL */
    private String imageUrl;
}
