package com.asset.finance.receipt.mapper;

import com.asset.finance.receipt.entity.FinReceipt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 收款单 Mapper
 */
@Mapper
public interface FinReceiptMapper extends BaseMapper<FinReceipt> {

    /**
     * 行级锁查询（核销事务内使用，必须在 @Transactional 方法中调用）
     */
    @Select("SELECT * FROM fin_receipt WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    FinReceipt selectByIdForUpdate(Long id);
}
