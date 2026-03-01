package com.asset.system.code.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 业务编码规则表 sys_code_rule */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_code_rule")
public class SysCodeRule extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则唯一标识键，业务层调用（如 project / contract） */
    private String ruleKey;

    /** 规则名称 */
    private String ruleName;

    /** 编码前缀，如 PRJ */
    private String prefix;

    /** 日期格式，如 yyyyMM，留空则不拼日期 */
    private String dateFormat;

    /** 分隔符，如 - */
    private String sep;

    /** 序列号位数（前补零），如 4 → 0001 */
    private Integer seqLength;

    /**
     * 重置周期：
     * 0-不重置  1-按年  2-按月  3-按日
     */
    private Integer resetType;

    /** 当前序列号 */
    private Long currentSeq;

    /** 当前周期字符串，用于判断是否需要重置（如 202603） */
    private String currentPeriod;

    /** 状态：0停用 1启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
