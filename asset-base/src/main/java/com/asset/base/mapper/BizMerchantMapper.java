package com.asset.base.mapper;

import com.asset.base.entity.BizMerchant;
import com.asset.base.model.dto.MerchantQuery;
import com.asset.base.model.vo.MerchantVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商家 Mapper
 */
@Mapper
public interface BizMerchantMapper extends BaseMapper<BizMerchant> {

    /**
     * 带条件分页查询商家列表
     *
     * @param page  分页参数
     * @param q     查询条件
     * @return 分页结果
     */
    IPage<MerchantVO> selectPageWithCond(Page<MerchantVO> page, @Param("q") MerchantQuery q);
}
