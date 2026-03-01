package com.asset.system.category.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统分类管理表 sys_category */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_category")
public class SysCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类维度，如 asset_type / format / area */
    private String categoryType;

    /** 父节点ID，0=根节点 */
    private Long parentId;

    /** 祖级路径，逗号分隔，如 0,1,3 */
    private String ancestors;

    /** 分类编码 */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 层级深度，从 1 开始 */
    private Integer level;

    /** 同级排序 */
    private Integer sortOrder;

    /** 状态：0停用 1启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
