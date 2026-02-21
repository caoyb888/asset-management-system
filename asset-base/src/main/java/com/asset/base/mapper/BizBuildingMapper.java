package com.asset.base.mapper;

import com.asset.base.model.dto.BuildingQuery;
import com.asset.base.model.vo.BuildingVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.asset.base.entity.BizBuilding;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 楼栋 Mapper
 */
@Mapper
public interface BizBuildingMapper extends BaseMapper<BizBuilding> {

    /**
     * 分页查询楼栋列表（含项目名称）
     *
     * @param page 分页参数
     * @param q    查询条件
     * @return 分页结果
     */
    IPage<BuildingVO> selectPageWithCond(Page<BuildingVO> page, @Param("q") BuildingQuery q);
}
