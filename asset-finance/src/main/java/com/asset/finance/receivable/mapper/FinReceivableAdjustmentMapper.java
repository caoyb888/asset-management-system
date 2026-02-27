package com.asset.finance.receivable.mapper;

import com.asset.finance.receivable.entity.FinReceivableAdjustment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinReceivableAdjustmentMapper extends BaseMapper<FinReceivableAdjustment> {

    /** 按 approvalId 加锁查询（回调事务内使用） */
    @Select("SELECT * FROM fin_receivable_adjustment WHERE approval_id=#{approvalId} AND is_deleted=0 FOR UPDATE")
    FinReceivableAdjustment selectByApprovalIdForUpdate(String approvalId);
}
