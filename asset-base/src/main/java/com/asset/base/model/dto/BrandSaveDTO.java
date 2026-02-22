package com.asset.base.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 品牌新增/编辑 DTO
 */
@Data
public class BrandSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 品牌编码 */
    private String brandCode;

    /** 品牌名(中) */
    @NotBlank(message = "品牌中文名不能为空")
    private String brandNameCn;

    /** 品牌名(英) */
    private String brandNameEn;

    /** 所属业态 */
    private String formatType;

    /** 品牌等级：1高端 2中端 3大众 */
    private Integer brandLevel;

    /** 合作关系：1直营 2加盟 3代理 */
    private Integer cooperationType;

    /** 经营性质：1餐饮 2零售 3娱乐 4服务 */
    private Integer businessNature;

    /** 连锁类型：1连锁 2单店 */
    private Integer chainType;

    /** 项目阶段 */
    private String projectStage;

    /** 集团名称 */
    private String groupName;

    /** 总部地址 */
    private String hqAddress;

    /** 主要分布城市 */
    private String mainCities;

    /** 网址 */
    private String website;

    /** 联系电话 */
    private String phone;

    /** 品牌类型：1MALL 2商街 */
    private Integer brandType;

    /** 平均租金(元/㎡·月) */
    private BigDecimal avgRent;

    /** 最低客单价(元) */
    private BigDecimal minCustomerPrice;

    /** 品牌简介 */
    private String brandIntro;

    /** 联系人列表 */
    private List<BrandContactDTO> contacts;
}
