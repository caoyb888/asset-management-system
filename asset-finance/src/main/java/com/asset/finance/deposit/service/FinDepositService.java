package com.asset.finance.deposit.service;

import com.asset.finance.deposit.dto.*;
import com.asset.finance.deposit.entity.FinDepositAccount;
import com.asset.finance.deposit.entity.FinDepositTransaction;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 保证金管理 Service
 */
public interface FinDepositService extends IService<FinDepositAccount> {

    /**
     * 查询合同保证金账户（含余额汇总卡片）
     * 若账户不存在则返回 null
     */
    DepositAccountVO getAccount(Long contractId);

    /**
     * 分页查询保证金流水
     */
    IPage<FinDepositTransaction> pageTransaction(DepositQueryDTO query);

    /**
     * 缴纳保证金（直接生效，无需审批）
     * 更新账户 balance + total_in，记录流水（status=1）
     */
    void payIn(DepositPayInDTO dto);

    /**
     * 申请冲抵应收
     * 余额校验 → 创建流水（status=0）→ 提交 OA 审批
     * 审批回调后才正式扣减余额
     */
    Long processOffset(DepositOffsetDTO dto);

    /**
     * 申请退款
     * 余额校验 → 创建流水（status=0）→ 提交 OA 审批
     */
    Long processRefund(DepositRefundDTO dto);

    /**
     * 申请罚没
     * 余额校验 → 创建流水（status=0）→ 提交 OA 审批
     */
    Long processForfeit(DepositForfeitDTO dto);

    /**
     * OA 审批回调（幂等）
     * 通过：扣减余额 + 更新 total_xxx + 更新流水 status=1
     * 驳回：流水 status=2，余额不变
     */
    void approveCallback(String approvalId, boolean approved);
}
