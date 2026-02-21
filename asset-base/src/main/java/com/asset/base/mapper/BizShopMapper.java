package com.asset.base.mapper;

import com.asset.base.entity.BizShop;
import com.asset.base.model.dto.ShopQuery;
import com.asset.base.model.vo.ShopVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商铺 Mapper
 */
@Mapper
public interface BizShopMapper extends BaseMapper<BizShop> {

    /**
     * 分页查询商铺列表（含项目/楼栋/楼层名称）
     *
     * @param page 分页参数
     * @param q    查询条件
     * @return 分页结果
     */
    IPage<ShopVO> selectPageWithCond(Page<ShopVO> page, @Param("q") ShopQuery q);
}
