package com.asset.base.model.dto;

import com.asset.base.entity.BizProject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 项目新增/编辑 DTO（新增和编辑共用，id为空时为新增）
 */
@Data
public class ProjectSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 项目编号 */
    @NotBlank(message = "项目编号不能为空")
    private String projectCode;

    /** 项目名称 */
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    /** 所属公司ID */
    @NotNull(message = "所属公司不能为空")
    private Long companyId;

    /** 所在省份 */
    private String province;

    /** 所在城市 */
    private String city;

    /** 项目地址 */
    private String address;

    /** 产权性质：1国有 2集体 3私有 4其他 */
    private Integer propertyType;

    /** 经营类型：1自持 2租赁 3合作 */
    private Integer businessType;

    /** 建筑面积(㎡) */
    @DecimalMin(value = "0", message = "建筑面积不能为负数")
    private BigDecimal buildingArea;

    /** 经营面积(㎡) */
    @DecimalMin(value = "0", message = "经营面积不能为负数")
    private BigDecimal operatingArea;

    /** 运营状态：0筹备 1开业 2停业 */
    private Integer operationStatus;

    /** 开业时间 */
    private LocalDate openingDate;

    /** 负责人ID */
    private Long managerId;

    /** 项目图片列表 */
    private List<BizProject.ImageUrl> imageUrls;
}
