package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商铺列表/详情 VO
 */
@Data
public class ShopVO {

    private Long id;
    private Long projectId;
    /** 项目名称（JOIN 查询） */
    private String projectName;

    private Long buildingId;
    /** 楼栋名称（JOIN 查询） */
    private String buildingName;

    private Long floorId;
    /** 楼层名称（JOIN 查询） */
    private String floorName;

    private String shopCode;

    private Integer shopType;
    /** 商铺类型名称（Service 层填充） */
    private String shopTypeName;

    private BigDecimal rentArea;
    private BigDecimal measuredArea;
    private BigDecimal buildingArea;
    private BigDecimal operatingArea;

    private Integer shopStatus;
    /** 商铺状态名称（Service 层填充） */
    private String shopStatusName;

    private Integer countLeasingRate;
    private Integer countRentalRate;
    private Integer countOpeningRate;

    private String signedFormat;
    private String plannedFormat;
    private String ownerName;
    private String ownerContact;
    private String ownerPhone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
