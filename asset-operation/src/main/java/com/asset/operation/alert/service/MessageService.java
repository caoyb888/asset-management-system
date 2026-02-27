package com.asset.operation.alert.service;

import com.asset.operation.alert.entity.OprAlertRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * 消息/预警发送服务接口
 * 提供单条发送、批量发送、取消预警能力
 */
public interface MessageService {

    /**
     * 单条预警发送（幂等：alert_type + target_id + alert_date + channel 组合唯一时跳过）
     *
     * @param alertType 预警类型（1合同到期/2应收到期）
     * @param targetId  预警目标ID（合同台账ID或应收计划ID）
     * @param alertDate 预警触发日期
     * @param channel   发送渠道（1站内信/2邮件/3短信）
     * @param remark    备注信息
     * @return 预警记录ID（已存在则返回已有记录ID）
     */
    Long send(Integer alertType, Long targetId, LocalDate alertDate, Integer channel, String remark);

    /**
     * 批量发送预警（逐条幂等校验，失败不影响其他）
     *
     * @param records 预警记录列表
     */
    void batchSend(List<OprAlertRecord> records);

    /**
     * 取消预警（将符合条件的记录 sentStatus 更新为 3-已取消）
     *
     * @param targetId  预警目标ID
     * @param alertType 预警类型（传 null 则取消该 targetId 的所有类型）
     */
    void cancel(Long targetId, Integer alertType);
}
