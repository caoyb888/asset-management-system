package com.asset.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告新增/编辑 DTO
 */
@Data
public class NoticeSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 内容 */
    private String content;

    /** 公告类型：1通知 2公告 3政策 */
    private Integer noticeType;

    /** 状态：0草稿 1发布 2下架 */
    private Integer status;

    /** 计划发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;
}
