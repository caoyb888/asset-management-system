package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 楼层列表/详情 VO
 */
@Data
public class FloorVO {

    private Long id;
    private Long projectId;
    private Long buildingId;

    /** 楼栋名称（JOIN 查询） */
    private String buildingName;

    private String floorCode;
    private String floorName;

    private Integer status;
    /** 状态名称（Service 层填充） */
    private String statusName;

    private BigDecimal buildingArea;
    private BigDecimal operatingArea;
    private String remark;
    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
