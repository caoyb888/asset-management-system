package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 楼栋列表/详情 VO
 */
@Data
public class BuildingVO {

    private Long id;
    private Long projectId;

    /** 项目名称（JOIN 查询） */
    private String projectName;

    private String buildingCode;
    private String buildingName;

    private Integer status;
    /** 状态名称（Service 层填充） */
    private String statusName;

    private BigDecimal buildingArea;
    private BigDecimal operatingArea;
    private Integer aboveFloors;
    private Integer belowFloors;
    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
