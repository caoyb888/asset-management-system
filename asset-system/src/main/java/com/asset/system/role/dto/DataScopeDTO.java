package com.asset.system.role.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 数据权限设置请求 */
@Data
public class DataScopeDTO {
    @NotNull(message = "数据范围不能为空")
    private Integer dataScope;
    /** dataScope=2（自定义）时必填的部门ID列表 */
    private List<Long> deptIds;
}
