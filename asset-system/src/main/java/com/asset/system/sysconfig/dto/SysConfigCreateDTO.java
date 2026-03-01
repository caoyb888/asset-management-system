package com.asset.system.sysconfig.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 系统参数 新增/编辑 DTO */
@Data
public class SysConfigCreateDTO {

    /** 编辑时携带 */
    private Long id;

    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @NotBlank(message = "配置名称不能为空")
    private String configName;

    @NotBlank(message = "配置值不能为空")
    private String configValue;

    /** 分组: basic/security/upload/other */
    private String configGroup;

    private String description;

    private Integer isBuiltIn;
}
