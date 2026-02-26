package com.asset.operation.alert.service.impl;

import com.asset.operation.alert.entity.OprAlertRecord;
import com.asset.operation.alert.mapper.OprAlertRecordMapper;
import com.asset.operation.alert.service.OprAlertRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprAlertRecord ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprAlertRecordServiceImpl extends ServiceImpl<OprAlertRecordMapper, OprAlertRecord>
        implements OprAlertRecordService {}
