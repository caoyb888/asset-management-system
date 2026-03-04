package com.asset.base.service;

import com.asset.base.entity.BizNotice;
import com.asset.base.entity.BizNoticeRead;
import com.asset.base.mapper.BizNoticeMapper;
import com.asset.base.mapper.BizNoticeReadMapper;
import com.asset.base.model.dto.NoticeSaveDTO;
import com.asset.base.model.vo.NoticeReadStatsVO;
import com.asset.base.model.vo.NoticeVO;
import com.asset.base.service.impl.BizNoticeServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通知公告 Service 单元测试（NTC-U-01 ~ NTC-U-09）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知公告 Service 单元测试")
class BizNoticeServiceTest {

    @Mock
    BizNoticeMapper noticeMapper;

    @Mock
    BizNoticeReadMapper noticeReadMapper;

    @Spy
    @InjectMocks
    BizNoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noticeService, "baseMapper", noticeMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-01 新增-状态=发布时记录发布时间
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-01 新增公告-status=1-自动设置publishTime")
    void createNotice_statusPublished_setsPublishTime() {
        NoticeSaveDTO dto = new NoticeSaveDTO();
        dto.setTitle("发布公告");
        dto.setStatus(1); // 发布

        doAnswer(inv -> {
            BizNotice notice = inv.getArgument(0);
            notice.setId(100L);
            return true;
        }).when(noticeService).save(any(BizNotice.class));

        noticeService.createNotice(dto);

        verify(noticeService).save(argThat(n -> ((BizNotice) n).getPublishTime() != null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-02 新增-草稿状态不设发布时间
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-02 新增公告-status=0-publishTime应为null")
    void createNotice_statusDraft_noPublishTime() {
        NoticeSaveDTO dto = new NoticeSaveDTO();
        dto.setTitle("草稿公告");
        dto.setStatus(0); // 草稿

        doAnswer(inv -> {
            BizNotice notice = inv.getArgument(0);
            notice.setId(101L);
            return true;
        }).when(noticeService).save(any(BizNotice.class));

        noticeService.createNotice(dto);

        verify(noticeService).save(argThat(n -> ((BizNotice) n).getPublishTime() == null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-03 编辑-变更为发布自动补时间
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-03 编辑公告-草稿变发布-自动补publishTime")
    void updateNotice_changeToPublish_setsPublishTime() {
        BizNotice existing = new BizNotice();
        existing.setId(1L);
        existing.setIsDeleted(0);
        existing.setStatus(0); // 草稿
        existing.setPublishTime(null);

        NoticeSaveDTO dto = new NoticeSaveDTO();
        dto.setTitle("变更为发布");
        dto.setStatus(1); // 变为发布

        doReturn(existing).when(noticeService).getById(1L);
        doReturn(true).when(noticeService).updateById(any());

        noticeService.updateNotice(1L, dto);

        verify(noticeService).updateById(argThat(n -> ((BizNotice) n).getPublishTime() != null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-04 编辑-已有发布时间不覆盖
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-04 编辑公告-已有publishTime-不重新设置")
    void updateNotice_alreadyPublished_keepOriginalTime() {
        java.time.LocalDateTime originalTime = java.time.LocalDateTime.of(2026, 1, 15, 9, 0, 0);

        BizNotice existing = new BizNotice();
        existing.setId(1L);
        existing.setIsDeleted(0);
        existing.setStatus(1); // 已发布
        existing.setPublishTime(originalTime);

        NoticeSaveDTO dto = new NoticeSaveDTO();
        dto.setTitle("再次编辑");
        dto.setStatus(1); // 保持发布状态

        doReturn(existing).when(noticeService).getById(1L);
        doReturn(true).when(noticeService).updateById(any());

        noticeService.updateNotice(1L, dto);

        // 发布时间应保持不变
        verify(noticeService).updateById(argThat(n ->
                originalTime.equals(((BizNotice) n).getPublishTime())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-05 发布操作-设 status=1 并记录发布时间
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-05 发布公告-status设为1且publishTime不为null")
    void publishNotice_setsStatus1AndPublishTime() {
        BizNotice existing = new BizNotice();
        existing.setId(1L);
        existing.setIsDeleted(0);
        existing.setStatus(0);
        existing.setPublishTime(null);

        doReturn(existing).when(noticeService).getById(1L);
        doReturn(true).when(noticeService).updateById(any());

        noticeService.publishNotice(1L);

        verify(noticeService).updateById(argThat(n -> {
            BizNotice notice = (BizNotice) n;
            return notice.getStatus() == 1 && notice.getPublishTime() != null;
        }));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-06 下架操作-设 status=2
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-06 下架公告-status设为2")
    void unpublishNotice_setsStatus2() {
        BizNotice existing = new BizNotice();
        existing.setId(1L);
        existing.setIsDeleted(0);
        existing.setStatus(1);

        doReturn(existing).when(noticeService).getById(1L);
        doReturn(true).when(noticeService).updateById(any());

        noticeService.unpublishNotice(1L);

        verify(noticeService).updateById(argThat(n -> ((BizNotice) n).getStatus() == 2));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-07 标记已读-幂等（DuplicateKeyException 被静默忽略）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-07 标记已读-重复调用抛DuplicateKeyException-静默忽略")
    void markAsRead_duplicateKey_ignoredException() {
        BizNotice notice = new BizNotice();
        notice.setId(1L);
        notice.setIsDeleted(0);

        doReturn(notice).when(noticeService).getById(1L);
        // 模拟重复键异常
        when(noticeReadMapper.insert(any(BizNoticeRead.class)))
                .thenThrow(new DuplicateKeyException("uk_notice_user"));

        // 不应向外传播异常
        noticeService.markAsRead(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-08 已读统计-当前用户已读
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-08 已读统计-当前用户已读-currentUserRead=true")
    void getReadStats_userHasRead_returnsTrue() {
        BizNotice notice = new BizNotice();
        notice.setId(1L);
        notice.setIsDeleted(0);

        doReturn(notice).when(noticeService).getById(1L);
        // 总阅读数 5
        when(noticeReadMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(5L)  // 第一次调用：总数
                .thenReturn(1L); // 第二次调用：用户已读

        NoticeReadStatsVO result = noticeService.getReadStats(1L);

        assertThat(result.getNoticeId()).isEqualTo(1L);
        assertThat(result.getReadCount()).isEqualTo(5L);
        assertThat(result.isCurrentUserRead()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NTC-U-09 已读统计-当前用户未读
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NTC-U-09 已读统计-当前用户未读-currentUserRead=false")
    void getReadStats_userNotRead_returnsFalse() {
        BizNotice notice = new BizNotice();
        notice.setId(1L);
        notice.setIsDeleted(0);

        doReturn(notice).when(noticeService).getById(1L);
        when(noticeReadMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(3L)  // 总数
                .thenReturn(0L); // 当前用户未读

        NoticeReadStatsVO result = noticeService.getReadStats(1L);

        assertThat(result.getReadCount()).isEqualTo(3L);
        assertThat(result.isCurrentUserRead()).isFalse();
    }
}
