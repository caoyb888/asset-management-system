package com.asset.finance.receipt.service;

import com.asset.finance.receipt.dto.ReceiptCreateDTO;
import com.asset.finance.receipt.dto.ReceiptDetailVO;
import com.asset.finance.receipt.dto.ReceiptQueryDTO;
import com.asset.finance.receipt.entity.FinReceipt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 收款管理 Service
 */
public interface FinReceiptService extends IService<FinReceipt> {

    /** 分页查询收款单列表 */
    IPage<ReceiptDetailVO> pageQuery(ReceiptQueryDTO query);

    /** 查看收款单详情（含拆分明细） */
    ReceiptDetailVO getDetailById(Long id);

    /** 新增收款单（校验拆分合计 == 总金额） */
    Long create(ReceiptCreateDTO dto);

    /** 编辑收款单（仅 status=0 待核销状态可编辑） */
    void update(Long id, ReceiptCreateDTO dto);

    /** 作废收款单（仅 status=0 可作废） */
    void cancel(Long id, String reason);

    /** 未名款项归名（绑定合同） */
    void bind(Long id, Long contractId);
}
