package com.asset.base.service;

import com.asset.base.entity.BizShop;
import com.asset.base.model.dto.ShopMergeDTO;
import com.asset.base.model.dto.ShopQuery;
import com.asset.base.model.dto.ShopSaveDTO;
import com.asset.base.model.dto.ShopSplitDTO;
import com.asset.base.model.vo.ShopVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商铺管理 Service
 */
public interface BizShopService extends IService<BizShop> {

    /** 分页查询商铺列表 */
    IPage<ShopVO> pageShop(ShopQuery query);

    /** 查询商铺详情 */
    ShopVO getShopById(Long id);

    /** 新增商铺 */
    Long createShop(ShopSaveDTO dto);

    /** 编辑商铺 */
    void updateShop(Long id, ShopSaveDTO dto);

    /** 删除商铺（逻辑删除） */
    void deleteShop(Long id);

    /** 拆分商铺 */
    void splitShop(ShopSplitDTO dto);

    /** 合并商铺 */
    void mergeShop(ShopMergeDTO dto);
}
