package com.asset.operation.alert.service;

import com.asset.operation.alert.dto.AlertQueryDTO;
import com.asset.operation.alert.entity.OprAlertRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** OprAlertRecord Service 接口 */
public interface OprAlertRecordService extends IService<OprAlertRecord> {

    /**
     * 分页查询预警记录
     *
     * @param query 查询条件（含分页参数）
     * @return 分页结果
     */
    IPage<OprAlertRecord> pageQuery(AlertQueryDTO query);

    /**
     * 手动取消预警记录
     *
     * @param id 预警记录ID
     */
    void cancelById(Long id);
}
