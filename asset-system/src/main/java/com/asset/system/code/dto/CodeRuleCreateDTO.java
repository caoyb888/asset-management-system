package com.asset.system.code.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 编码规则 新增/编辑 DTO */
@Data
public class CodeRuleCreateDTO {

    /** 编辑时携带 */
    private Long id;

    @NotBlank(message = "规则标识不能为空")
    private String ruleKey;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    /** 前缀，允许为空 */
    private String prefix;

    /** 日期格式，如 yyyyMM，留空则不拼日期 */
    private String dateFormat;

    /** 分隔符 */
    private String sep;

    @NotNull(message = "序列号位数不能为空")
    @Min(value = 1, message = "序列号位数最小为1")
    private Integer seqLength;

    /** 重置周期：0不重置 1按年 2按月 3按日 */
    @NotNull(message = "重置周期不能为空")
    private Integer resetType;

    private Integer status;
    private String remark;
}
