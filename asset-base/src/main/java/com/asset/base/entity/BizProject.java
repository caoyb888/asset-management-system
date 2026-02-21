package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 项目实体 - 对应 biz_project 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_project", autoResultMap = true)
public class BizProject extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目编号（同一项目删除后可重建，复合唯一索引） */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 所属公司ID */
    private Long companyId;

    /** 所在省份 */
    private String province;

    /** 所在城市 */
    private String city;

    /** 项目地址 */
    private String address;

    /**
     * 产权性质
     * 1-国有 2-集体 3-私有 4-其他
     */
    private Integer propertyType;

    /**
     * 经营类型
     * 1-自持 2-租赁 3-合作
     */
    private Integer businessType;

    /** 建筑面积(㎡) */
    private BigDecimal buildingArea;

    /** 经营面积(㎡) */
    private BigDecimal operatingArea;

    /**
     * 运营状态
     * 0-筹备 1-开业 2-停业
     */
    private Integer operationStatus;

    /** 开业时间 */
    private LocalDate openingDate;

    /** 负责人ID */
    private Long managerId;

    /**
     * 项目图片JSON数组
     * 格式: [{"url":"...","name":"外观图","sort":1}]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ImageUrl> imageUrls;

    /** 图片 URL 内嵌类 */
    @Data
    public static class ImageUrl {
        private String url;
        private String name;
        private Integer sort;
    }
}
