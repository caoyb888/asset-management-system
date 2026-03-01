package com.asset.system.sysconfig.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统参数配置表 sys_config */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键（唯一） */
    private String configKey;

    /** 配置名称 */
    private String configName;

    /** 配置值 */
    private String configValue;

    /** 配置分组: basic/security/upload/other */
    private String configGroup;

    /** 说明 */
    private String description;

    /** 是否内置: 1是（不可删除）0否 */
    private Integer isBuiltIn;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
