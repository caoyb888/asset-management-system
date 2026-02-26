package com.asset.operation.change.service.impl;

import com.asset.operation.change.entity.OprContractChangeType;
import com.asset.operation.change.mapper.OprContractChangeTypeMapper;
import com.asset.operation.change.service.OprContractChangeTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprContractChangeType ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprContractChangeTypeServiceImpl extends ServiceImpl<OprContractChangeTypeMapper, OprContractChangeType>
        implements OprContractChangeTypeService {}
