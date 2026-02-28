package com.asset.finance.deposit.mapper;

import com.asset.finance.deposit.entity.FinDepositAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinDepositAccountMapper extends BaseMapper<FinDepositAccount> {

    /** 按合同ID查询账户（普通，不加锁） */
    @Select("SELECT * FROM fin_deposit_account WHERE contract_id=#{contractId} AND is_deleted=0 LIMIT 1")
    FinDepositAccount selectByContractId(Long contractId);

    /** 按合同ID加行级锁查询（余额更新事务内使用） */
    @Select("SELECT * FROM fin_deposit_account WHERE contract_id=#{contractId} AND is_deleted=0 FOR UPDATE")
    FinDepositAccount selectByContractIdForUpdate(Long contractId);

    /** 按ID加行级锁查询 */
    @Select("SELECT * FROM fin_deposit_account WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    FinDepositAccount selectByIdForUpdate(Long id);
}
