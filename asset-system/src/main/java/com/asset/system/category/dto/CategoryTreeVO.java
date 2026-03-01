package com.asset.system.category.dto;

import lombok.Data;

import java.util.List;

/** 分类树节点 VO */
@Data
public class CategoryTreeVO {

    private Long id;
    private String categoryType;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer level;
    private Integer sortOrder;
    private Integer status;
    private String remark;

    /** 子节点列表 */
    private List<CategoryTreeVO> children;
}
