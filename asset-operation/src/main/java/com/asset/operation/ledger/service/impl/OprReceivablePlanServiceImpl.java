package com.asset.operation.ledger.service.impl;

import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.ledger.service.OprReceivablePlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** 应收计划 ServiceImpl */
@Slf4j
@Service
public class OprReceivablePlanServiceImpl extends ServiceImpl<OprReceivablePlanMapper, OprReceivablePlan>
        implements OprReceivablePlanService {

    @Override
    public List<OprReceivablePlan> listByLedgerId(Long ledgerId) {
        return list(new LambdaQueryWrapper<OprReceivablePlan>()
                .eq(OprReceivablePlan::getLedgerId, ledgerId)
                .ne(OprReceivablePlan::getStatus, 3)  // 排除已作废
                .orderByAsc(OprReceivablePlan::getDueDate)
        );
    }
}
