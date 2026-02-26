package com.asset.operation.revenue.service;

import com.asset.operation.revenue.dto.FloatingRentDetailVO;
import com.asset.operation.revenue.entity.OprFloatingRent;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** 浮动租金 Service 接口 */
public interface OprFloatingRentService extends IService<OprFloatingRent> {

    /**
     * 分页查询浮动租金列表
     * @param contractId 合同ID（可选）
     * @param calcMonth  月份（YYYY-MM，可选）
     * @param pageNum    页码
     * @param pageSize   每页条数
     */
    IPage<OprFloatingRent> pageQuery(Long contractId, String calcMonth, int pageNum, int pageSize);

    /**
     * 浮动租金详情（含阶梯明细）
     * @param id 浮动租金记录ID
     */
    FloatingRentDetailVO detail(Long id);

    /**
     * 手动生成应收计划（仅当 receivable_id 为空时可执行）
     * @param id 浮动租金记录ID
     * @return 生成的应收计划ID
     */
    Long generateReceivable(Long id);
}
