package com.asset.system.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 岗位新增/更新请求 */
@Data
public class PostCreateDTO {
    private Long id;
    @NotBlank(message = "岗位编码不能为空")
    private String postCode;
    @NotBlank(message = "岗位名称不能为空")
    private String postName;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
