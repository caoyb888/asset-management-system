package com.asset.operation.ledger.service;

import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 应收计划 Service 接口
 */
public interface OprReceivablePlanService extends IService<OprReceivablePlan> {

    /**
     * 查询台账下所有有效应收计划（排除已作废）
     */
    List<OprReceivablePlan> listByLedgerId(Long ledgerId);
}
