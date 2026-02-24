package com.asset.investment.intention.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 意向协议新增/编辑请求体
 */
@Data
public class IntentionSaveDTO {

    /** 意向协议名称 */
    @NotBlank(message = "意向协议名称不能为空")
    private String intentionName;

    /** 所属项目ID */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** 商家ID（可选） */
    private Long merchantId;

    /** 意向品牌ID（可选） */
    private Long brandId;

    /** 签约主体 */
    private String signingEntity;

    /** 计租方案ID */
    private Long rentSchemeId;

    /** 交付日 */
    private LocalDate deliveryDate;

    /** 装修开始日期 */
    private LocalDate decorationStart;

    /** 装修结束日期 */
    private LocalDate decorationEnd;

    /** 开业日 */
    private LocalDate openingDate;

    /** 合同开始日期 */
    private LocalDate contractStart;

    /** 合同结束日期 */
    private LocalDate contractEnd;

    /**
     * 支付周期
     * 1月付/2两月付/3季付/4四月付/5半年付/6年付
     */
    private Integer paymentCycle;

    /**
     * 账期模式
     * 1预付/2当期/3后付
     */
    private Integer billingMode;

    /** 协议文本内容 */
    private String agreementText;
}
