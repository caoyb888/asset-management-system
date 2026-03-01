package com.asset.system.post.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 岗位查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageQuery {
    private String postCode;
    private String postName;
    private Integer status;
}
