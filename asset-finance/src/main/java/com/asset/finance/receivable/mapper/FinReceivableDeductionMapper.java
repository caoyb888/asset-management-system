package com.asset.finance.receivable.mapper;

import com.asset.finance.receivable.entity.FinReceivableDeduction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinReceivableDeductionMapper extends BaseMapper<FinReceivableDeduction> {

    /** 按 approvalId 加锁查询（回调事务内使用） */
    @Select("SELECT * FROM fin_receivable_deduction WHERE approval_id=#{approvalId} AND is_deleted=0 FOR UPDATE")
    FinReceivableDeduction selectByApprovalIdForUpdate(String approvalId);
}
