package com.asset.operation.change.service.impl;

import com.asset.operation.change.entity.OprContractChangeDetail;
import com.asset.operation.change.mapper.OprContractChangeDetailMapper;
import com.asset.operation.change.service.OprContractChangeDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprContractChangeDetail ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprContractChangeDetailServiceImpl extends ServiceImpl<OprContractChangeDetailMapper, OprContractChangeDetail>
        implements OprContractChangeDetailService {}
