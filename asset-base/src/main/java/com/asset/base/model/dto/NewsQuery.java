package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 新闻资讯分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NewsQuery extends PageQuery {

    /** 标题（模糊查询） */
    private String title;

    /** 资讯分类：1新闻 2政策 3招商动态 4服务指南 */
    private Integer category;

    /** 状态：0草稿 1上架 2下架 */
    private Integer status;
}
