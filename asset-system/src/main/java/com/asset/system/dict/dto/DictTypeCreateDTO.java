package com.asset.system.dict.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 字典类型新增/更新请求 */
@Data
public class DictTypeCreateDTO {
    private Long id;
    @NotBlank(message = "字典名称不能为空")
    private String dictName;
    @NotBlank(message = "字典类型标识不能为空")
    private String dictType;
    private Integer status;
    private String remark;
}
