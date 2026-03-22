package com.asset.system.extfield.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 扩展字段定义 返回 VO */
@Data
public class ExtFieldVO {

    private Long id;
    private String moduleCode;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private List<Map<String, String>> optionsJson;
    private Boolean required;
    private Integer sortOrder;
    private Boolean showInList;
    private Boolean showInForm;
    private String defaultVal;
    private String placeholder;
    private Integer maxLength;
    private BigDecimal minVal;
    private BigDecimal maxVal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
