package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 品牌实体 - 对应 biz_brand 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_brand")
public class BizBrand extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品牌编码 */
    private String brandCode;

    /** 品牌名(中) */
    private String brandNameCn;

    /** 品牌名(英) */
    private String brandNameEn;

    /** 所属业态 */
    private String formatType;

    /**
     * 品牌等级
     * 1-高端 2-中端 3-大众
     */
    private Integer brandLevel;

    /**
     * 合作关系
     * 1-直营 2-加盟 3-代理
     */
    private Integer cooperationType;

    /**
     * 经营性质
     * 1-餐饮 2-零售 3-娱乐 4-服务
     */
    private Integer businessNature;

    /**
     * 连锁类型
     * 1-连锁 2-单店
     */
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

    /**
     * 品牌类型
     * 1-MALL 2-商街
     */
    private Integer brandType;

    /** 平均租金(元/㎡·月) */
    private BigDecimal avgRent;

    /** 最低客单价(元) */
    private BigDecimal minCustomerPrice;

    /** 品牌简介 */
    private String brandIntro;
}
