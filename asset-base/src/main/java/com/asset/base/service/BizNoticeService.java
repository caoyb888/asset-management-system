package com.asset.base.service;

import com.asset.base.entity.BizNotice;
import com.asset.base.model.dto.NoticeQuery;
import com.asset.base.model.dto.NoticeSaveDTO;
import com.asset.base.model.vo.NoticeVO;
import com.asset.base.model.vo.NoticeReadStatsVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 通知公告 Service
 */
public interface BizNoticeService extends IService<BizNotice> {

    /** 分页查询通知公告列表 */
    IPage<NoticeVO> pageNotice(NoticeQuery query);

    /** 查询通知公告详情 */
    NoticeVO getNoticeById(Long id);

    /** 新增通知公告 */
    Long createNotice(NoticeSaveDTO dto);

    /** 编辑通知公告 */
    void updateNotice(Long id, NoticeSaveDTO dto);

    /** 删除通知公告（逻辑删除） */
    void deleteNotice(Long id);

    /** 发布通知公告（status→1，publishTime=now） */
    void publishNotice(Long id);

    /** 下架通知公告（status→2） */
    void unpublishNotice(Long id);

    /** 当前用户标记已读 */
    void markAsRead(Long noticeId);

    /** 查询公告已读统计（已读人数） */
    NoticeReadStatsVO getReadStats(Long noticeId);
}
