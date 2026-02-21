package com.asset.base.converter;

import com.asset.base.entity.BizBuilding;
import com.asset.base.model.dto.BuildingSaveDTO;
import com.asset.base.model.vo.BuildingVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 楼栋对象转换器（MapStruct 生成实现类）
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BuildingConverter {

    /** DTO → Entity（新增） */
    BizBuilding toEntity(BuildingSaveDTO dto);

    /** DTO → Entity（编辑：仅更新非null字段） */
    void updateEntity(BuildingSaveDTO dto, @MappingTarget BizBuilding entity);

    /** Entity → VO */
    BuildingVO toVO(BizBuilding entity);
}
