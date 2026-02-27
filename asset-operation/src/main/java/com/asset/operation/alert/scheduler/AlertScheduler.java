package com.asset.operation.alert.scheduler;

import com.asset.operation.alert.service.MessageService;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 预警定时任务
 * 每日检查合同到期和应收逾期，触发站内信预警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final OprContractLedgerMapper contractLedgerMapper;
    private final OprReceivablePlanMapper receivablePlanMapper;
    private final MessageService messageService;

    /**
     * 合同到期预警：每天早上 8:00 执行
     * 检查 30 天内到期的进行中合同台账，按档位生成预警备注
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkContractExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(30);

        // 查询 status=0（进行中）且 contract_end 在 [today, today+30] 的台账
        List<OprContractLedger> ledgers = contractLedgerMapper.selectList(
                new LambdaQueryWrapper<OprContractLedger>()
                        .eq(OprContractLedger::getStatus, 0)
                        .ge(OprContractLedger::getContractEnd, today)
                        .le(OprContractLedger::getContractEnd, thresholdDate)
        );

        log.info("[AlertScheduler] 合同到期预警检查，共发现 {} 条即将到期台账", ledgers.size());

        for (OprContractLedger ledger : ledgers) {
            long daysLeft = today.until(ledger.getContractEnd()).getDays();
            String remark;
            if (daysLeft <= 7) {
                remark = String.format("合同即将到期，剩余 %d 天（紧急）", daysLeft);
            } else if (daysLeft <= 15) {
                remark = String.format("合同即将到期，剩余 %d 天（提醒）", daysLeft);
            } else {
                remark = String.format("合同即将到期，剩余 %d 天", daysLeft);
            }
            try {
                messageService.send(1, ledger.getId(), today, 1, remark);
            } catch (Exception e) {
                log.error("[AlertScheduler] 合同到期预警发送失败，台账ID={}", ledger.getId(), e);
            }
        }
    }

    /**
     * 应收逾期预警：每天早上 9:00 执行
     * 检查 due_date 已过且 status=0（待收）的应收计划
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkReceivableOverdue() {
        LocalDate today = LocalDate.now();

        // 查询 status=0（待收）且 due_date < today 的逾期应收计划
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(
                new LambdaQueryWrapper<OprReceivablePlan>()
                        .eq(OprReceivablePlan::getStatus, 0)
                        .lt(OprReceivablePlan::getDueDate, today)
        );

        log.info("[AlertScheduler] 应收逾期预警检查，共发现 {} 条逾期应收", plans.size());

        for (OprReceivablePlan plan : plans) {
            try {
                messageService.send(2, plan.getId(), today, 1, "应收逾期预警");
            } catch (Exception e) {
                log.error("[AlertScheduler] 应收逾期预警发送失败，计划ID={}", plan.getId(), e);
            }
        }
    }
}
