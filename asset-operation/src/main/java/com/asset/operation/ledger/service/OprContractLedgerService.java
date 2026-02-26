package com.asset.operation.ledger.service;

import com.asset.operation.ledger.dto.AuditDTO;
import com.asset.operation.ledger.dto.LedgerDetailVO;
import com.asset.operation.ledger.dto.LedgerQueryDTO;
import com.asset.operation.ledger.dto.OneTimePaymentDTO;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 合同台账 Service 接口
 */
public interface OprContractLedgerService extends IService<OprContractLedger> {

    /**
     * 分页查询台账列表
     */
    IPage<OprContractLedger> pageQuery(LedgerQueryDTO query);

    /**
     * 查询台账详情（含应收计划、关联信息）
     */
    LedgerDetailVO getDetailById(Long id);

    /**
     * 根据招商合同ID创建台账（招商合同审批通过后调用）
     *
     * @param contractId 招商合同ID
     * @return 创建的台账ID
     */
    Long generateFromContract(Long contractId);

    /**
     * 双签确认
     */
    void confirmDoubleSign(Long ledgerId);

    /**
     * 生成应收计划（调用 ReceivablePlanGenerator）
     *
     * @return 生成的应收计划数量
     */
    int generateReceivable(Long ledgerId);

    /**
     * 审核台账（通过/驳回）
     */
    void audit(Long ledgerId, AuditDTO dto);

    /**
     * 手动推送应收至财务系统
     */
    void pushReceivable(Long ledgerId);

    /**
     * 录入一次性首款
     */
    void addOneTimePayment(Long ledgerId, OneTimePaymentDTO dto);
}
