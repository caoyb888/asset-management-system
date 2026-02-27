package com.asset.finance.prepayment.mapper;

import com.asset.finance.prepayment.entity.FinPrepayAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 预收款账户 Mapper
 */
@Mapper
public interface FinPrepayAccountMapper extends BaseMapper<FinPrepayAccount> {

    /**
     * 按合同ID查询预收款账户（通用账户，fee_item_id 为空）
     */
    @Select("SELECT * FROM fin_prepay_account WHERE contract_id=#{contractId} AND fee_item_id IS NULL AND is_deleted=0 LIMIT 1")
    FinPrepayAccount selectByContractId(Long contractId);

    /**
     * 行级锁查询（余额更新事务内使用）
     */
    @Select("SELECT * FROM fin_prepay_account WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    FinPrepayAccount selectByIdForUpdate(Long id);
}
