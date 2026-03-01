package com.asset.system.category.dto;

import lombok.Data;

/** 分类查询参数（返回树形，不分页） */
@Data
public class CategoryQueryDTO {

    /** 分类维度（必填） */
    private String categoryType;

    /** 名称关键字（可选，过滤用） */
    private String categoryName;

    /** 状态筛选（null=全部） */
    private Integer status;
}
