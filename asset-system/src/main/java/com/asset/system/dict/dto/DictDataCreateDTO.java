package com.asset.system.dict.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 字典数据新增/更新请求 */
@Data
public class DictDataCreateDTO {
    private Long id;
    @NotBlank(message = "字典类型不能为空")
    private String dictType;
    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;
    @NotBlank(message = "字典值不能为空")
    private String dictValue;
    private String cssClass;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
