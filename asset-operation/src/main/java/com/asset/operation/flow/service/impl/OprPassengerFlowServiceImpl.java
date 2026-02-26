package com.asset.operation.flow.service.impl;

import com.asset.operation.flow.entity.OprPassengerFlow;
import com.asset.operation.flow.mapper.OprPassengerFlowMapper;
import com.asset.operation.flow.service.OprPassengerFlowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprPassengerFlow ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprPassengerFlowServiceImpl extends ServiceImpl<OprPassengerFlowMapper, OprPassengerFlow>
        implements OprPassengerFlowService {}
