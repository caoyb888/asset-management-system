package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻资讯列表/详情 VO
 */
@Data
public class NewsVO {

    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 资讯分类：1新闻 2政策 3招商动态 4服务指南 */
    private Integer category;

    /** 资讯分类名称（Service 层填充） */
    private String categoryName;

    /**
     * 状态：0草稿 1上架 2下架
     */
    private Integer status;

    /** 状态名称（Service 层填充） */
    private String statusName;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
