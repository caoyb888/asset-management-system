package com.asset.investment.opening.mapper;

import com.asset.investment.opening.entity.InvOpeningApproval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvOpeningApprovalMapper extends BaseMapper<InvOpeningApproval> {

    /**
     * 开业审批分页查询（LEFT JOIN 合同/商家/商铺，返回名称字段）
     */
    IPage<InvOpeningApproval> pageQueryWithCondition(
            Page<InvOpeningApproval> page,
            @Param("projectId") Long projectId,
            @Param("status") Integer status,
            @Param("contractId") Long contractId);
}
