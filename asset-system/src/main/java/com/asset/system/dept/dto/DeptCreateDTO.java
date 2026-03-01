package com.asset.system.dept.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 部门新增/更新请求 */
@Data
public class DeptCreateDTO {
    private Long id;
    private Long parentId;
    @NotBlank(message = "部门名称不能为空")
    private String deptName;
    private String deptCode;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
