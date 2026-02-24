package com.asset.investment.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 招商合同新增/编辑请求体
 */
@Data
public class ContractSaveDTO {

    /** 合同名称（必填） */
    @NotBlank(message = "合同名称不能为空")
    private String contractName;

    /** 合同类型（必填）：1标准租赁合同 2临时租赁合同 3补充协议 */
    @NotNull(message = "合同类型不能为空")
    private Integer contractType;

    /** 所属项目ID（必填） */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** 商家ID */
    private Long merchantId;

    /** 品牌ID */
    private Long brandId;

    /** 签约主体（乙方名称） */
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
     * 支付周期：1月付/2两月付/3季付/4四月付/5半年付/6年付
     */
    private Integer paymentCycle;

    /**
     * 账期模式：1预付/2当期/3后付
     */
    private Integer billingMode;

    /** 合同文本 */
    private String contractText;
}
