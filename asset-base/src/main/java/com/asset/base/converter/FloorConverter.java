package com.asset.base.converter;

import com.asset.base.entity.BizFloor;
import com.asset.base.model.dto.FloorSaveDTO;
import com.asset.base.model.vo.FloorVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 楼层对象转换器（MapStruct 生成实现类）
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface FloorConverter {

    /** DTO → Entity（新增） */
    BizFloor toEntity(FloorSaveDTO dto);

    /** DTO → Entity（编辑：仅更新非null字段） */
    void updateEntity(FloorSaveDTO dto, @MappingTarget BizFloor entity);

    /** Entity → VO */
    FloorVO toVO(BizFloor entity);
}
