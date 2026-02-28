package com.asset.base.job;

import com.asset.base.entity.BizNotice;
import com.asset.base.service.BizNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知公告定时发布任务
 *
 * <p>每分钟扫描一次 biz_notice 表，将 scheduled_time <= 当前时间 且 status=0（草稿）
 * 的公告自动发布（status→1，publish_time=now）。</p>
 *
 * <p>在 XXL-Job 管理台注册：
 * <ul>
 *   <li>JobHandler：scheduledPublishJob</li>
 *   <li>Cron：0 * * * * ?（每分钟执行一次）</li>
 *   <li>执行器：asset-base-job</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledPublishJobHandler {

    private final BizNoticeService noticeService;

    @XxlJob("scheduledPublishJob")
    public void scheduledPublish() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[scheduledPublishJob] 开始扫描待定时发布公告，当前时间={}", now);

        // 查询 scheduled_time <= now 且 status=0（草稿）的公告
        List<BizNotice> pendingList = noticeService.list(
                new LambdaQueryWrapper<BizNotice>()
                        .eq(BizNotice::getIsDeleted, 0)
                        .eq(BizNotice::getStatus, 0)
                        .isNotNull(BizNotice::getScheduledTime)
                        .le(BizNotice::getScheduledTime, now)
        );

        if (pendingList.isEmpty()) {
            log.info("[scheduledPublishJob] 无待发布公告");
            return;
        }

        int successCount = 0;
        for (BizNotice notice : pendingList) {
            try {
                noticeService.publishNotice(notice.getId());
                successCount++;
                log.info("[scheduledPublishJob] 公告[{}]「{}」定时发布成功", notice.getId(), notice.getTitle());
            } catch (Exception e) {
                log.error("[scheduledPublishJob] 公告[{}]发布失败: {}", notice.getId(), e.getMessage());
            }
        }

        log.info("[scheduledPublishJob] 本次扫描共{}条待发布，成功{}", pendingList.size(), successCount);
    }
}
