package com.asset.operation.ledger.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 应收计划表 - 对应 opr_receivable_plan */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_receivable_plan")
public class OprReceivablePlan extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 合同台账ID */
    private Long ledgerId;
    /** 合同ID */
    private Long contractId;
    /** 商铺ID */
    private Long shopId;
    /** 收款项目ID */
    private Long feeItemId;
    /** 费项名称（快照，避免费项修改影响历史数据） */
    private String feeName;
    /** 账期开始 */
    private LocalDate billingStart;
    /** 账期结束 */
    private LocalDate billingEnd;
    /** 应收日期/付款截止日 */
    private LocalDate dueDate;
    /** 应收金额 */
    private BigDecimal amount;
    /** 已收金额 */
    private BigDecimal receivedAmount;
    /** 状态（0待收/1部分收款/2已收/3已作废） */
    private Integer status;
    /** 推送状态（0未推送/1已推送） */
    private Integer pushStatus;
    /** 推送财务时间 */
    private LocalDateTime pushTime;
    /** 推送幂等键（receivable_{id}_{version}），财务系统据此防重复处理 */
    private String pushIdempotentKey;
    /** 来源（1合同生成/2变更生成/3浮动租金/4一次性录入） */
    private Integer sourceType;
    /** 版本号（变更后递增，用于追踪变更历史） */
    private Integer version;
}
