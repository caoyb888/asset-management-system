package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 品牌列表/详情 VO
 */
@Data
public class BrandVO {

    private Long id;
    private String brandCode;
    private String brandNameCn;
    private String brandNameEn;
    private String formatType;

    private Integer brandLevel;
    /** 品牌等级名称（Service 层填充） */
    private String brandLevelName;

    private Integer cooperationType;
    /** 合作关系名称（Service 层填充） */
    private String cooperationTypeName;

    private Integer businessNature;
    /** 经营性质名称（Service 层填充） */
    private String businessNatureName;

    private Integer chainType;
    /** 连锁类型名称（Service 层填充） */
    private String chainTypeName;

    private String projectStage;
    private String groupName;
    private String hqAddress;
    private String mainCities;
    private String website;
    private String phone;

    private Integer brandType;
    /** 品牌类型名称（Service 层填充） */
    private String brandTypeName;

    private BigDecimal avgRent;
    private BigDecimal minCustomerPrice;
    private String brandIntro;

    /** 联系人列表（详情接口返回） */
    private List<BrandContactVO> contacts;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
