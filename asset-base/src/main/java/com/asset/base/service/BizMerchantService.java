package com.asset.base.service;

import com.asset.base.entity.BizMerchant;
import com.asset.base.model.dto.MerchantQuery;
import com.asset.base.model.dto.MerchantSaveDTO;
import com.asset.base.model.vo.MerchantVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商家管理 Service
 */
public interface BizMerchantService extends IService<BizMerchant> {

    /** 分页查询商家列表 */
    IPage<MerchantVO> pageMerchant(MerchantQuery query);

    /** 查询商家详情（含联系人、开票信息） */
    MerchantVO getMerchantById(Long id);

    /** 新增商家（含联系人、开票信息） */
    Long createMerchant(MerchantSaveDTO dto);

    /** 编辑商家（含联系人、开票信息） */
    void updateMerchant(Long id, MerchantSaveDTO dto);

    /** 删除商家（逻辑删除） */
    void deleteMerchant(Long id);
}
