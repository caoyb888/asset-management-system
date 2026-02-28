package com.asset.finance.voucher.service;

import com.asset.finance.voucher.dto.VoucherCreateDTO;
import com.asset.finance.voucher.dto.VoucherDetailVO;
import com.asset.finance.voucher.dto.VoucherQueryDTO;
import com.asset.finance.voucher.entity.FinVoucher;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 凭证管理 Service
 *
 * <p>凭证状态机：0(待审核) → 1(已审核) → 2(已上传)
 * <p>只有 status=0 的凭证可删除；只有 status=1 的凭证可上传。
 */
public interface FinVoucherService extends IService<FinVoucher> {

    /**
     * 分页查询凭证列表（含项目名称、状态名称）
     */
    IPage<VoucherDetailVO> pageQuery(VoucherQueryDTO query);

    /**
     * 查询凭证详情（含分录列表）
     */
    VoucherDetailVO getDetail(Long id);

    /**
     * 手动创建凭证
     * 校验借贷平衡：Σ debitAmount == Σ creditAmount
     */
    Long createVoucher(VoucherCreateDTO dto);

    /**
     * 基于收款单自动生成收款凭证（标准两条分录）
     * 借：银行存款 1002  贷：应收账款 1122
     */
    Long generateFromReceipt(Long receiptId);

    /**
     * 审核凭证（status: 0 → 1）
     */
    void audit(Long id);

    /**
     * 上传凭证到财务系统（status: 1 → 2，记录 uploadTime）
     * 模拟调用，实际对接财务系统时替换此方法体
     */
    void upload(Long id);

    /**
     * 删除凭证（仅 status=0 时允许，级联删除分录）
     */
    void deleteVoucher(Long id);
}
