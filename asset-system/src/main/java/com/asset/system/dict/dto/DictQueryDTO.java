package com.asset.system.dict.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictQueryDTO extends PageQuery {
    private String dictName;
    private String dictType;
    private Integer status;
}
