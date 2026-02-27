package com.asset.operation.alert.service.impl;

import com.asset.operation.alert.dto.AlertQueryDTO;
import com.asset.operation.alert.entity.OprAlertRecord;
import com.asset.operation.alert.mapper.OprAlertRecordMapper;
import com.asset.operation.alert.service.OprAlertRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OprAlertRecord ServiceImpl */
@Slf4j
@Service
public class OprAlertRecordServiceImpl extends ServiceImpl<OprAlertRecordMapper, OprAlertRecord>
        implements OprAlertRecordService {

    @Override
    public IPage<OprAlertRecord> pageQuery(AlertQueryDTO query) {
        LambdaQueryWrapper<OprAlertRecord> wrapper = new LambdaQueryWrapper<OprAlertRecord>()
                .eq(query.getAlertType() != null, OprAlertRecord::getAlertType, query.getAlertType())
                .eq(query.getSentStatus() != null, OprAlertRecord::getSentStatus, query.getSentStatus())
                .eq(query.getTargetId() != null, OprAlertRecord::getTargetId, query.getTargetId())
                .ge(query.getAlertDateFrom() != null, OprAlertRecord::getAlertDate, query.getAlertDateFrom())
                .le(query.getAlertDateTo() != null, OprAlertRecord::getAlertDate, query.getAlertDateTo())
                .orderByDesc(OprAlertRecord::getAlertDate);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelById(Long id) {
        OprAlertRecord record = getById(id);
        if (record == null) {
            throw new IllegalArgumentException("预警记录不存在，id=" + id);
        }
        record.setSentStatus(3);
        record.setRemark("手动取消");
        updateById(record);
        log.info("[OprAlertRecord] 手动取消预警记录，id={}", id);
    }
}
