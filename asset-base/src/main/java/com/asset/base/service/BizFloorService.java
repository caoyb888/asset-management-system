package com.asset.base.service;

import com.asset.base.entity.BizFloor;
import com.asset.base.model.dto.FloorQuery;
import com.asset.base.model.dto.FloorSaveDTO;
import com.asset.base.model.vo.FloorVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 楼层管理 Service
 */
public interface BizFloorService extends IService<BizFloor> {

    /** 分页查询楼层列表 */
    IPage<FloorVO> pageFloor(FloorQuery query);

    /** 查询楼层详情 */
    FloorVO getFloorById(Long id);

    /** 新增楼层 */
    Long createFloor(FloorSaveDTO dto);

    /** 编辑楼层 */
    void updateFloor(Long id, FloorSaveDTO dto);

    /** 删除楼层（逻辑删除） */
    void deleteFloor(Long id);
}
