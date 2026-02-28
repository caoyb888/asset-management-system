package com.asset.finance.deposit.mapper;

import com.asset.finance.deposit.entity.FinDepositTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinDepositTransactionMapper extends BaseMapper<FinDepositTransaction> {

    /** 按 approvalId 加锁查询（审批回调事务内使用） */
    @Select("SELECT * FROM fin_deposit_transaction WHERE approval_id=#{approvalId} AND is_deleted=0 FOR UPDATE")
    FinDepositTransaction selectByApprovalIdForUpdate(String approvalId);
}
