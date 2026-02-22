package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandQuery extends PageQuery {

    /** 品牌名称（中文，模糊） */
    private String brandNameCn;

    /** 所属业态（模糊） */
    private String formatType;

    /** 品牌等级：1高端 2中端 3大众 */
    private Integer brandLevel;

    /** 合作关系：1直营 2加盟 3代理 */
    private Integer cooperationType;

    /** 经营性质：1餐饮 2零售 3娱乐 4服务 */
    private Integer businessNature;

    /** 品牌类型：1MALL 2商街 */
    private Integer brandType;
}
