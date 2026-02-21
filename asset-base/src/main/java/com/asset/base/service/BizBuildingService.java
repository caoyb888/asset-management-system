package com.asset.base.service;

import com.asset.base.entity.BizBuilding;
import com.asset.base.model.dto.BuildingQuery;
import com.asset.base.model.dto.BuildingSaveDTO;
import com.asset.base.model.vo.BuildingVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 楼栋管理 Service
 */
public interface BizBuildingService extends IService<BizBuilding> {

    /** 分页查询楼栋列表 */
    IPage<BuildingVO> pageBuilding(BuildingQuery query);

    /** 查询楼栋详情 */
    BuildingVO getBuildingById(Long id);

    /** 新增楼栋 */
    Long createBuilding(BuildingSaveDTO dto);

    /** 编辑楼栋 */
    void updateBuilding(Long id, BuildingSaveDTO dto);

    /** 删除楼栋（逻辑删除） */
    void deleteBuilding(Long id);
}
