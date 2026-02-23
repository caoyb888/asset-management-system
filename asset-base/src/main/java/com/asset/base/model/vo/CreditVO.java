package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商家诚信记录 VO
 */
@Data
public class CreditVO {

    private Long id;
    private Long merchantId;
    private Integer recordType;
    /** 记录类型名称（Service 层填充：1→"好评" 2→"差评" 3→"违约" 4→"其他"） */
    private String recordTypeName;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    private Long operatorId;
    private String attachmentUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
