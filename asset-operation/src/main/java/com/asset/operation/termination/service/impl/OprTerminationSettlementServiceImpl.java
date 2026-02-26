package com.asset.operation.termination.service.impl;

import com.asset.operation.termination.entity.OprTerminationSettlement;
import com.asset.operation.termination.mapper.OprTerminationSettlementMapper;
import com.asset.operation.termination.service.OprTerminationSettlementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprTerminationSettlement ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprTerminationSettlementServiceImpl extends ServiceImpl<OprTerminationSettlementMapper, OprTerminationSettlement>
        implements OprTerminationSettlementService {}
