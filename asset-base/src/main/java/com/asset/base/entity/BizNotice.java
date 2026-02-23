package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知公告实体 - 对应 biz_notice 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_notice")
public class BizNotice extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容（LONGTEXT） */
    private String content;

    /**
     * 公告类型
     * 1-通知 2-公告 3-政策
     */
    private Integer noticeType;

    /**
     * 状态
     * 0-草稿 1-发布 2-下架
     */
    private Integer status;

    /** 计划发布时间 */
    private LocalDateTime scheduledTime;

    /** 实际发布时间 */
    private LocalDateTime publishTime;
}
