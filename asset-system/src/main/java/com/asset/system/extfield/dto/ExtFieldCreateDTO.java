package com.asset.system.extfield.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 扩展字段定义 新增/修改 DTO（新增/修改共用，Service 层对新增必填字段做额外校验） */
@Data
public class ExtFieldCreateDTO {

    /** 编辑时携带 */
    private Long id;

    /** 模块编码（新增时必填，修改时忽略） */
    private String moduleCode;

    /**
     * 字段标识：小写英文字母开头，只含小写字母/数字/下划线，最长 64 字符
     * 新增时必填，创建后不可修改
     */
    @Pattern(regexp = "^[a-z][a-z0-9_]{0,62}$",
             message = "字段标识只能以小写字母开头，仅含小写字母、数字和下划线，且不超过64个字符")
    private String fieldKey;

    /** 字段显示名称（中文） */
    private String fieldLabel;

    /**
     * 字段类型：text/textarea/number/date/select/radio/checkbox
     * 新增时必填
     */
    private String fieldType;

    /** 选项列表（select/radio/checkbox 时应提供） */
    private List<Map<String, String>> optionsJson;

    /** 是否必填，默认 false */
    private Boolean required;

    /** 排序序号，默认 0 */
    private Integer sortOrder;

    /** 是否在列表页展示，默认 false */
    private Boolean showInList;

    /** 是否在表单页展示，默认 true */
    private Boolean showInForm;

    /** 默认值 */
    private String defaultVal;

    /** 输入提示 */
    private String placeholder;

    /** 最大长度（text/textarea 适用） */
    private Integer maxLength;

    /** 最小值（number 适用） */
    private BigDecimal minVal;

    /** 最大值（number 适用） */
    private BigDecimal maxVal;
}
