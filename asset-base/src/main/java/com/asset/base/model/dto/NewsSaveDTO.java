package com.asset.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻资讯新增/编辑 DTO
 */
@Data
public class NewsSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 内容 */
    private String content;

    /** 资讯分类：1新闻 2政策 3招商动态 4服务指南 */
    private Integer category;

    /** 状态：0草稿 1上架 2下架 */
    private Integer status;

    /** 发布时间（可手动指定） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
}
