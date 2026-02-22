package com.asset.base.mapper;

import com.asset.base.entity.BizBrand;
import com.asset.base.model.dto.BrandQuery;
import com.asset.base.model.vo.BrandVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 品牌 Mapper
 */
@Mapper
public interface BizBrandMapper extends BaseMapper<BizBrand> {

    /**
     * 带条件分页查询品牌列表
     *
     * @param page  分页参数
     * @param q     查询条件
     * @return 分页结果
     */
    IPage<BrandVO> selectPageWithCond(Page<BrandVO> page, @Param("q") BrandQuery q);
}
