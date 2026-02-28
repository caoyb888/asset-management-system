package com.asset.finance.voucher.mapper;

import com.asset.finance.voucher.entity.FinVoucherEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FinVoucherEntryMapper extends BaseMapper<FinVoucherEntry> {

    /** 查询凭证的所有分录，按 id 排序 */
    @Select("SELECT * FROM fin_voucher_entry WHERE voucher_id=#{voucherId} AND is_deleted=0 ORDER BY id")
    List<FinVoucherEntry> selectByVoucherId(Long voucherId);
}
