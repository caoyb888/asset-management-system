package com.asset.finance.prepayment.service;

import com.asset.finance.prepayment.dto.*;
import com.asset.finance.prepayment.entity.FinPrepayAccount;
import com.asset.finance.prepayment.entity.FinPrepayTransaction;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 预收款管理 Service
 *
 * <p>预收款所有操作直接生效（无需 OA 审批）。
 * <p>来源：① 核销溢出自动转入（WriteOffServiceImpl 调用 addBalance）；② 手动录入。
 */
public interface FinPrepaymentService extends IService<FinPrepayAccount> {

    /**
     * 查询合同预收款账户余额（账户不存在返回 null）
     */
    PrepayAccountVO getAccount(Long contractId);

    /**
     * 分页查询预收款流水
     */
    IPage<FinPrepayTransaction> pageTransaction(PrepayQueryDTO query);

    /**
     * 手动录入预收款（transType=1，直接加余额）
     */
    void deposit(PrepayDepositDTO dto);

    /**
     * 增加余额（供核销溢出时内部调用）
     * accountId 为空时按 contractId 查找或自动创建账户
     */
    void addBalance(Long contractId, Long accountId, java.math.BigDecimal amount, String sourceCode, String remark);

    /**
     * 抵冲应收（transType=2）
     * 余额校验 → 扣减余额 → 更新应收 receivedAmount/outstandingAmount/status → 记流水
     */
    void offset(PrepayOffsetDTO dto);

    /**
     * 退款（transType=3）
     * 余额校验 → 扣减余额 → 记流水
     */
    void refund(PrepayRefundDTO dto);
}
