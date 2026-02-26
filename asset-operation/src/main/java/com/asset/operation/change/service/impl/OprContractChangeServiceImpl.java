package com.asset.operation.change.service.impl;

import com.asset.operation.change.entity.OprContractChange;
import com.asset.operation.change.mapper.OprContractChangeMapper;
import com.asset.operation.change.service.OprContractChangeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprContractChange ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprContractChangeServiceImpl extends ServiceImpl<OprContractChangeMapper, OprContractChange>
        implements OprContractChangeService {}
