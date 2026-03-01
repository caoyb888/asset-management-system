package com.asset.system.dept.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 移动机构（变更上级）请求 */
@Data
public class MoveDeptDTO {
    /** 新的父部门ID（0=提升为顶级） */
    @NotNull(message = "目标父部门不能为空")
    private Long targetParentId;
}
