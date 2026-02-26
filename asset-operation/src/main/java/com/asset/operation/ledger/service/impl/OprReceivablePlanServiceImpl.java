package com.asset.operation.ledger.service.impl;

import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.ledger.service.OprReceivablePlanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprReceivablePlan ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprReceivablePlanServiceImpl extends ServiceImpl<OprReceivablePlanMapper, OprReceivablePlan>
        implements OprReceivablePlanService {}
