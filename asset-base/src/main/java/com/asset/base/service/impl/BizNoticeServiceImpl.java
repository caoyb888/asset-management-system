package com.asset.base.service.impl;

import com.asset.base.entity.BizNotice;
import com.asset.base.entity.BizNoticeRead;
import com.asset.base.mapper.BizNoticeMapper;
import com.asset.base.mapper.BizNoticeReadMapper;
import com.asset.base.model.dto.NoticeQuery;
import com.asset.base.model.dto.NoticeSaveDTO;
import com.asset.base.model.vo.NoticeReadStatsVO;
import com.asset.base.model.vo.NoticeVO;
import com.asset.base.service.BizNoticeService;
import com.asset.common.exception.BizException;
import com.asset.common.security.util.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知公告 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizNoticeServiceImpl
        extends ServiceImpl<BizNoticeMapper, BizNotice>
        implements BizNoticeService {

    private final BizNoticeReadMapper noticeReadMapper;

    private static final Map<Integer, String> NOTICE_TYPE_MAP = Map.of(
            1, "通知", 2, "公告", 3, "政策");

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            0, "草稿", 1, "已发布", 2, "已下架");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<NoticeVO> pageNotice(NoticeQuery query) {
        LambdaQueryWrapper<BizNotice> wrapper = new LambdaQueryWrapper<BizNotice>()
                .eq(BizNotice::getIsDeleted, 0)
                .like(StringUtils.hasText(query.getTitle()), BizNotice::getTitle, query.getTitle())
                .eq(query.getNoticeType() != null, BizNotice::getNoticeType, query.getNoticeType())
                .eq(query.getStatus() != null, BizNotice::getStatus, query.getStatus())
                .orderByDesc(BizNotice::getCreatedAt);
        Page<BizNotice> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<BizNotice> entityPage = page(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public NoticeVO getNoticeById(Long id) {
        BizNotice notice = getById(id);
        if (notice == null || notice.getIsDeleted() == 1) {
            throw new BizException("通知公告不存在或已删除");
        }
        return toVO(notice);
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNotice(NoticeSaveDTO dto) {
        BizNotice entity = new BizNotice();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        // 若保存时直接设为发布状态，记录发布时间
        if (Integer.valueOf(1).equals(entity.getStatus())) {
            entity.setPublishTime(LocalDateTime.now());
        }
        save(entity);
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(Long id, NoticeSaveDTO dto) {
        BizNotice existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("通知公告不存在或已删除");
        }
        BeanUtils.copyProperties(dto, existing, "id");
        // 若状态变更为发布且原先没有发布时间，记录发布时间
        if (Integer.valueOf(1).equals(existing.getStatus()) && existing.getPublishTime() == null) {
            existing.setPublishTime(LocalDateTime.now());
        }
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        BizNotice existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("通知公告不存在或已删除");
        }
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 发布 / 下架                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(Long id) {
        BizNotice existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("通知公告不存在或已删除");
        }
        existing.setStatus(1);
        existing.setPublishTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishNotice(Long id) {
        BizNotice existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("通知公告不存在或已删除");
        }
        existing.setStatus(2);
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 已读追踪                                                               */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long noticeId) {
        BizNotice notice = getById(noticeId);
        if (notice == null || notice.getIsDeleted() == 1) {
            throw new BizException("通知公告不存在或已删除");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        // uk_notice_user 唯一约束保证幂等，捕获重复键忽略即可
        BizNoticeRead record = new BizNoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setReadTime(LocalDateTime.now());
        record.setIsDeleted(0);
        record.setCreatedAt(LocalDateTime.now());
        try {
            noticeReadMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            // 已读过，幂等处理
        }
    }

    @Override
    public NoticeReadStatsVO getReadStats(Long noticeId) {
        BizNotice notice = getById(noticeId);
        if (notice == null || notice.getIsDeleted() == 1) {
            throw new BizException("通知公告不存在或已删除");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        long readCount = noticeReadMapper.selectCount(
                new LambdaQueryWrapper<BizNoticeRead>()
                        .eq(BizNoticeRead::getNoticeId, noticeId)
                        .eq(BizNoticeRead::getIsDeleted, 0)
        );
        boolean currentUserRead = noticeReadMapper.selectCount(
                new LambdaQueryWrapper<BizNoticeRead>()
                        .eq(BizNoticeRead::getNoticeId, noticeId)
                        .eq(BizNoticeRead::getUserId, userId)
                        .eq(BizNoticeRead::getIsDeleted, 0)
        ) > 0;
        NoticeReadStatsVO vo = new NoticeReadStatsVO();
        vo.setNoticeId(noticeId);
        vo.setReadCount(readCount);
        vo.setCurrentUserRead(currentUserRead);
        return vo;
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    /** Entity → VO */
    private NoticeVO toVO(BizNotice entity) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(entity, vo);
        fillEnumNames(vo);
        return vo;
    }

    /** 填充枚举名称 */
    private void fillEnumNames(NoticeVO vo) {
        if (vo.getNoticeType() != null) {
            vo.setNoticeTypeName(NOTICE_TYPE_MAP.getOrDefault(vo.getNoticeType(), "未知"));
        }
        if (vo.getStatus() != null) {
            vo.setStatusName(STATUS_MAP.getOrDefault(vo.getStatus(), "未知"));
        }
    }
}
