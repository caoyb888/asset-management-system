package com.asset.base.model.vo;

import com.asset.base.entity.BizProject;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目列表/详情 VO
 */
@Data
public class ProjectVO {

    private Long id;
    private String projectCode;
    private String projectName;

    private Long companyId;
    /** 所属公司名称（JOIN 查询） */
    private String companyName;

    private String province;
    private String city;
    private String address;

    private Integer propertyType;
    private String propertyTypeName;

    private Integer businessType;
    private String businessTypeName;

    private BigDecimal buildingArea;
    private BigDecimal operatingArea;

    private Integer operationStatus;
    private String operationStatusName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate openingDate;

    private Long managerId;
    /** 负责人姓名（JOIN 查询） */
    private String managerName;

    private List<BizProject.ImageUrl> imageUrls;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
