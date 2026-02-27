package com.asset.finance.receipt.service;

import com.asset.finance.receipt.dto.*;
import com.asset.finance.receipt.entity.FinWriteOff;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 核销管理 Service
 */
public interface FinWriteOffService extends IService<FinWriteOff> {

    /**
     * 分页查询核销单列表
     */
    IPage<WriteOffDetailVO> pageQuery(WriteOffQueryDTO query);

    /**
     * 查询可核销的应收记录（按合同ID）
     * 仅返回 status IN (0,1) 的应收记录
     */
    List<WritableReceivableVO> queryWritableReceivables(Long contractId);

    /**
     * 提交核销申请
     * 校验：收款单余额 >= 本次核销总额
     * 提交 OA 审批，返回核销单ID
     */
    Long submitWriteOff(Long receiptId, List<WriteOffDetailItemDTO> items, Integer writeOffType);

    /**
     * 审批回调（核心事务方法）
     * approved=true：更新应收已收/欠费，超额转预存款，更新收款单余额/状态
     * approved=false：驳回，仅更新核销单状态
     *
     * @param approvalId OA审批流程ID
     * @param approved   是否通过
     * @param comment    审批意见
     */
    void approveCallback(String approvalId, boolean approved, String comment);

    /**
     * 撤销核销单（仅 status=0 待审核状态可撤销）
     */
    void cancelWriteOff(Long id);

    /**
     * 查看核销单详情
     */
    WriteOffDetailVO getDetailById(Long id);
}
