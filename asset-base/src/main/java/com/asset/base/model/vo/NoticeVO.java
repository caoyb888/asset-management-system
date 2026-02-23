package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告列表/详情 VO
 */
@Data
public class NoticeVO {

    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 公告类型：1通知 2公告 3政策 */
    private Integer noticeType;

    /** 公告类型名称（Service 层填充） */
    private String noticeTypeName;

    /**
     * 状态：0草稿 1已发布 2已下架
     */
    private Integer status;

    /** 状态名称（Service 层填充） */
    private String statusName;

    /** 计划发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;

    /** 实际发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
