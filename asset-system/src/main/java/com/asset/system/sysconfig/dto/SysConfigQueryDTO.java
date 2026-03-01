package com.asset.system.sysconfig.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统参数查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigQueryDTO extends PageQuery {

    /** 配置键（模糊） */
    private String configKey;

    /** 配置名称（模糊） */
    private String configName;

    /** 分组 */
    private String configGroup;
}
