package com.asset.base.converter;

import com.asset.base.entity.BizMerchant;
import com.asset.base.entity.BizMerchantContact;
import com.asset.base.entity.BizMerchantInvoice;
import com.asset.base.model.dto.MerchantContactDTO;
import com.asset.base.model.dto.MerchantInvoiceDTO;
import com.asset.base.model.dto.MerchantSaveDTO;
import com.asset.base.model.vo.MerchantContactVO;
import com.asset.base.model.vo.MerchantInvoiceVO;
import com.asset.base.model.vo.MerchantVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 商家对象转换器（MapStruct 生成实现类）
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MerchantConverter {

    /** DTO → Entity（新增） */
    BizMerchant toEntity(MerchantSaveDTO dto);

    /** DTO → Entity（编辑：仅更新非null字段） */
    void updateEntity(MerchantSaveDTO dto, @MappingTarget BizMerchant entity);

    /** Entity → VO */
    MerchantVO toVO(BizMerchant entity);

    /** 联系人 DTO → Entity */
    BizMerchantContact toContactEntity(MerchantContactDTO dto);

    /** 联系人 Entity → VO */
    MerchantContactVO toContactVO(BizMerchantContact entity);

    /** 开票信息 DTO → Entity */
    BizMerchantInvoice toInvoiceEntity(MerchantInvoiceDTO dto);

    /** 开票信息 Entity → VO */
    MerchantInvoiceVO toInvoiceVO(BizMerchantInvoice entity);
}
