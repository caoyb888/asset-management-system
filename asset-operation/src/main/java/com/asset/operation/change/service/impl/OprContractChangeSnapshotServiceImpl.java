package com.asset.operation.change.service.impl;

import com.asset.operation.change.entity.OprContractChangeSnapshot;
import com.asset.operation.change.mapper.OprContractChangeSnapshotMapper;
import com.asset.operation.change.service.OprContractChangeSnapshotService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprContractChangeSnapshot ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprContractChangeSnapshotServiceImpl extends ServiceImpl<OprContractChangeSnapshotMapper, OprContractChangeSnapshot>
        implements OprContractChangeSnapshotService {}
