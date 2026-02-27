package com.asset.finance.receivable.mapper;

import com.asset.finance.receivable.entity.FinReceivable;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 应收台账 Mapper
 */
@Mapper
public interface FinReceivableMapper extends BaseMapper<FinReceivable> {

    /**
     * 行级锁查询（核销事务内使用，必须在 @Transactional 方法中调用）
     */
    @Select("SELECT * FROM fin_receivable WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    FinReceivable selectByIdForUpdate(Long id);
}
