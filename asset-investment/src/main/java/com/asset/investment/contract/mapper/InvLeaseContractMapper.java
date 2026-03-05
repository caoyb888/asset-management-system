package com.asset.investment.contract.mapper;

import com.asset.investment.contract.dto.ContractQueryDTO;
import com.asset.investment.contract.entity.InvLeaseContract;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvLeaseContractMapper extends BaseMapper<InvLeaseContract> {

    /**
     * 合同分页查询（LEFT JOIN 项目/商家/品牌，返回名称字段）
     */
    IPage<InvLeaseContract> pageQueryWithCondition(Page<InvLeaseContract> page,
                                                    @Param("q") ContractQueryDTO q);
}
