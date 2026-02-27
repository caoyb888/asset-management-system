package com.asset.operation.alert.service.impl;

import com.asset.operation.alert.entity.OprAlertRecord;
import com.asset.operation.alert.mapper.OprAlertRecordMapper;
import com.asset.operation.alert.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息/预警发送服务实现（Mock版本）
 * <p>
 * 生产环境集成说明：
 * - 站内信（channel=1）：写入 sys_message 表或通过 WebSocket 推送
 * - 邮件（channel=2）：调用邮件网关 API（如 JavaMail / SendGrid）
 * - 短信（channel=3）：调用短信服务商 API（如阿里云短信）
 * 替换 {@link #mockSend} 方法中的 TODO 注释处即可
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final OprAlertRecordMapper alertRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long send(Integer alertType, Long targetId, LocalDate alertDate, Integer channel, String remark) {
        // 幂等查重：alert_type + target_id + alert_date + channel 唯一
        OprAlertRecord existing = alertRecordMapper.selectOne(
                new LambdaQueryWrapper<OprAlertRecord>()
                        .eq(OprAlertRecord::getAlertType, alertType)
                        .eq(OprAlertRecord::getTargetId, targetId)
                        .eq(OprAlertRecord::getAlertDate, alertDate)
                        .eq(OprAlertRecord::getChannel, channel)
                        .ne(OprAlertRecord::getSentStatus, 3) // 排除已取消
                        .last("LIMIT 1")
        );
        if (existing != null) {
            log.debug("[消息服务] 预警已存在，跳过重复发送，recordId={}", existing.getId());
            return existing.getId();
        }

        // 插入预警记录（待发送状态）
        OprAlertRecord record = new OprAlertRecord();
        record.setAlertType(alertType);
        record.setTargetId(targetId);
        record.setAlertDate(alertDate);
        record.setChannel(channel);
        record.setSentStatus(0);
        record.setRemark(remark);
        alertRecordMapper.insert(record);

        // 执行发送（Mock）
        try {
            mockSend(record);
            alertRecordMapper.update(null, new LambdaUpdateWrapper<OprAlertRecord>()
                    .eq(OprAlertRecord::getId, record.getId())
                    .set(OprAlertRecord::getSentStatus, 1)
                    .set(OprAlertRecord::getSentTime, LocalDateTime.now())
            );
            log.info("[消息服务] 预警发送成功，recordId={}，channel={}", record.getId(), channel);
        } catch (Exception e) {
            alertRecordMapper.update(null, new LambdaUpdateWrapper<OprAlertRecord>()
                    .eq(OprAlertRecord::getId, record.getId())
                    .set(OprAlertRecord::getSentStatus, 2)
                    .set(OprAlertRecord::getRemark, "发送失败：" + e.getMessage())
            );
            log.error("[消息服务] 预警发送失败，recordId={}，error={}", record.getId(), e.getMessage());
        }

        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSend(List<OprAlertRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (OprAlertRecord r : records) {
            send(r.getAlertType(), r.getTargetId(), r.getAlertDate(), r.getChannel(), r.getRemark());
        }
        log.info("[消息服务] 批量发送预警完成，共 {} 条", records.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long targetId, Integer alertType) {
        LambdaUpdateWrapper<OprAlertRecord> wrapper = new LambdaUpdateWrapper<OprAlertRecord>()
                .eq(OprAlertRecord::getTargetId, targetId)
                .ne(OprAlertRecord::getSentStatus, 3)
                .set(OprAlertRecord::getSentStatus, 3)
                .set(OprAlertRecord::getRemark, "手动取消");
        if (alertType != null) {
            wrapper.eq(OprAlertRecord::getAlertType, alertType);
        }
        int rows = alertRecordMapper.update(null, wrapper);
        log.info("[消息服务] 取消预警完成，targetId={}，alertType={}，影响 {} 条", targetId, alertType, rows);
    }

    /**
     * Mock 发送实现（仅打印日志）
     * 生产环境替换此方法为真实网关调用
     */
    private void mockSend(OprAlertRecord record) {
        String channelName = switch (record.getChannel()) {
            case 1 -> "站内信";
            case 2 -> "邮件";
            case 3 -> "短信";
            default -> "未知渠道(" + record.getChannel() + ")";
        };
        log.info("[消息服务-Mock] 模拟发送{}，alertType={}，targetId={}，alertDate={}",
                channelName, record.getAlertType(), record.getTargetId(), record.getAlertDate());
        // TODO: 替换为真实发送逻辑
        // 示例（站内信）：sysMessageService.push(record.getTargetId(), buildMessage(record));
        // 示例（短信）：smsGateway.send(mobilePhone, buildSmsContent(record));
    }
}
