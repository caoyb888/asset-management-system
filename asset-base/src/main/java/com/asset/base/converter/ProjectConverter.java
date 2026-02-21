package com.asset.base.converter;

import com.asset.base.entity.BizProject;
import com.asset.base.model.dto.ProjectSaveDTO;
import com.asset.base.model.vo.ProjectVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 项目对象转换器（MapStruct 生成实现类）
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProjectConverter {

    /** DTO → Entity（新增） */
    BizProject toEntity(ProjectSaveDTO dto);

    /** DTO → Entity（编辑：仅更新非null字段，保留原有审计字段） */
    void updateEntity(ProjectSaveDTO dto, @MappingTarget BizProject entity);

    /** Entity → VO（枚举名称字段在 Service 层手动填充） */
    ProjectVO toVO(BizProject entity);
}
