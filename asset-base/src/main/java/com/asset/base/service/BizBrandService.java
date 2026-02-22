package com.asset.base.service;

import com.asset.base.entity.BizBrand;
import com.asset.base.model.dto.BrandQuery;
import com.asset.base.model.dto.BrandSaveDTO;
import com.asset.base.model.vo.BrandVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 品牌管理 Service
 */
public interface BizBrandService extends IService<BizBrand> {

    /** 分页查询品牌列表 */
    IPage<BrandVO> pageBrand(BrandQuery query);

    /** 查询品牌详情（含联系人） */
    BrandVO getBrandById(Long id);

    /** 新增品牌（含联系人） */
    Long createBrand(BrandSaveDTO dto);

    /** 编辑品牌（含联系人） */
    void updateBrand(Long id, BrandSaveDTO dto);

    /** 删除品牌（逻辑删除） */
    void deleteBrand(Long id);
}
