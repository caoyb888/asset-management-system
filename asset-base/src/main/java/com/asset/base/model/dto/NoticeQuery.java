package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知公告分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQuery extends PageQuery {

    /** 标题（模糊查询） */
    private String title;

    /** 公告类型：1通知 2公告 3政策 */
    private Integer noticeType;

    /** 状态：0草稿 1已发布 2已下架 */
    private Integer status;
}
