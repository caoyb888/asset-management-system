package com.asset.system.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 分类 新增/编辑 DTO */
@Data
public class CategoryCreateDTO {

    /** 编辑时携带 */
    private Long id;

    @NotBlank(message = "分类维度不能为空")
    private String categoryType;

    /** 父节点ID，0=根节点 */
    private Long parentId;

    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    private Integer sortOrder;
    private Integer status;
    private String remark;
}
