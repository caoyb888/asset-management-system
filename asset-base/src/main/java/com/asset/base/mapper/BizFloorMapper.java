package com.asset.base.mapper;

import com.asset.base.entity.BizFloor;
import com.asset.base.model.dto.FloorQuery;
import com.asset.base.model.vo.FloorVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 楼层 Mapper
 */
@Mapper
public interface BizFloorMapper extends BaseMapper<BizFloor> {

    /**
     * 分页查询楼层列表（含楼栋名称）
     *
     * @param page 分页参数
     * @param q    查询条件
     * @return 分页结果
     */
    IPage<FloorVO> selectPageWithCond(Page<FloorVO> page, @Param("q") FloorQuery q);
}
