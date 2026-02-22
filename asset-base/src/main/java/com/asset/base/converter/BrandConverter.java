package com.asset.base.converter;

import com.asset.base.entity.BizBrand;
import com.asset.base.entity.BizBrandContact;
import com.asset.base.model.dto.BrandContactDTO;
import com.asset.base.model.dto.BrandSaveDTO;
import com.asset.base.model.vo.BrandContactVO;
import com.asset.base.model.vo.BrandVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 品牌对象转换器（MapStruct 生成实现类）
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BrandConverter {

    /** DTO → Entity（新增） */
    BizBrand toEntity(BrandSaveDTO dto);

    /** DTO → Entity（编辑：仅更新非null字段） */
    void updateEntity(BrandSaveDTO dto, @MappingTarget BizBrand entity);

    /** Entity → VO */
    BrandVO toVO(BizBrand entity);

    /** 联系人 DTO → Entity */
    BizBrandContact toContactEntity(BrandContactDTO dto);

    /** 联系人 Entity → VO */
    BrandContactVO toContactVO(BizBrandContact entity);
}
