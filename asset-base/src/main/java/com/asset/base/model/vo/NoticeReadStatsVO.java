package com.asset.base.model.vo;

import lombok.Data;

/**
 * 公告已读统计 VO
 */
@Data
public class NoticeReadStatsVO {

    /** 公告ID */
    private Long noticeId;

    /** 已读人数 */
    private long readCount;

    /** 当前用户是否已读 */
    private boolean currentUserRead;
}
