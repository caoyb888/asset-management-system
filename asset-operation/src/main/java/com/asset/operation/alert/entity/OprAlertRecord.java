package com.asset.operation.alert.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 预警记录表（合同/应收到期多渠道预警，防重复发送）- 对应 opr_alert_record */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_alert_record")
public class OprAlertRecord extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 预警类型（1合同到期/2应收到期） */
    private Integer alertType;
    /** 预警目标ID（合同台账ID或应收计划ID） */
    private Long targetId;
    /** 预警触发日期 */
    private LocalDate alertDate;
    /** 发送渠道（1站内信/2邮件/3短信） */
    private Integer channel;
    /** 发送状态（0待发送/1已发送/2发送失败/3已取消） */
    private Integer sentStatus;
    /** 实际发送时间 */
    private LocalDateTime sentTime;
    /** 备注（失败原因、取消原因等） */
    private String remark;
}
