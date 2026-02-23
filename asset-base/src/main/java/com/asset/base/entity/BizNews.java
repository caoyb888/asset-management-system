package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 新闻资讯实体 - 对应 biz_news 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_news")
public class BizNews extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容（LONGTEXT） */
    private String content;

    /**
     * 资讯分类
     * 1-新闻 2-政策 3-招商动态 4-服务指南
     */
    private Integer category;

    /**
     * 状态
     * 0-草稿 1-上架 2-下架
     */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;
}
