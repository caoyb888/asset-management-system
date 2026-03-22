package com.asset.system.extfield.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户自定义扩展字段元数据定义 sys_ext_field_def
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_ext_field_def", autoResultMap = true)
public class SysExtFieldDef extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块编码（project/shop/brand/merchant/intention/contract/ledger/change/receivable/receipt） */
    private String moduleCode;

    /** 字段标识（英文+下划线，用作 JSON key，创建后不可修改） */
    private String fieldKey;

    /** 字段显示名称（中文） */
    private String fieldLabel;

    /** 字段类型：text/textarea/number/date/select/radio/checkbox */
    private String fieldType;

    /**
     * 选项列表，仅 select/radio/checkbox 使用
     * 格式：[{"label":"显示名","value":"存储值"}]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> optionsJson;

    /** 是否必填：false-否 true-是 */
    private Boolean required;

    /** 排序序号（升序） */
    private Integer sortOrder;

    /** 是否在列表页展示 */
    private Boolean showInList;

    /** 是否在表单页展示 */
    private Boolean showInForm;

    /** 默认值（字符串形式存储） */
    private String defaultVal;

    /** 输入提示文本 */
    private String placeholder;

    /** 最大长度（text/textarea 适用） */
    private Integer maxLength;

    /** 最小值（number 适用） */
    private BigDecimal minVal;

    /** 最大值（number 适用） */
    private BigDecimal maxVal;
}
