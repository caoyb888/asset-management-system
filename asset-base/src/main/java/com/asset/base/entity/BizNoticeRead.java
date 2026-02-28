package com.asset.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告阅读记录实体 - 对应 biz_notice_read 表
 */
@Data
@TableName("biz_notice_read")
public class BizNoticeRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告ID */
    private Long noticeId;

    /** 用户ID */
    private Long userId;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 逻辑删除：0正常 1删除 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
