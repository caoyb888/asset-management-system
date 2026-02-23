package com.asset.base.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 商家诚信记录新增 DTO
 */
@Data
public class CreditSaveDTO {

    /** 记录类型（必填）：1-好评 2-差评 3-违约 4-其他 */
    @NotNull(message = "记录类型不能为空")
    private Integer recordType;

    /** 记录内容 */
    private String content;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 附件地址 */
    private String attachmentUrl;
}
