package com.asset.operation.termination.service.impl;

import com.asset.operation.termination.entity.OprContractTermination;
import com.asset.operation.termination.mapper.OprContractTerminationMapper;
import com.asset.operation.termination.service.OprContractTerminationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprContractTermination ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprContractTerminationServiceImpl extends ServiceImpl<OprContractTerminationMapper, OprContractTermination>
        implements OprContractTerminationService {}
