package com.asset.base.converter;

import com.asset.base.entity.BizShop;
import com.asset.base.model.dto.ShopSaveDTO;
import com.asset.base.model.vo.ShopVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 商铺对象转换器（MapStruct 生成实现类）
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ShopConverter {

    /** DTO → Entity（新增） */
    BizShop toEntity(ShopSaveDTO dto);

    /** DTO → Entity（编辑：仅更新非null字段） */
    void updateEntity(ShopSaveDTO dto, @MappingTarget BizShop entity);

    /** Entity → VO */
    ShopVO toVO(BizShop entity);
}
