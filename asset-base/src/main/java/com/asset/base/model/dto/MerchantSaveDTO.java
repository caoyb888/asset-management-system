package com.asset.base.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 商家新增/编辑 DTO
 */
@Data
public class MerchantSaveDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 所属项目ID */
    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    /** 商家编号 */
    private String merchantCode;

    /** 商家名称 */
    @NotBlank(message = "商家名称不能为空")
    private String merchantName;

    /** 商家属性：1个体户 2企业 */
    private Integer merchantAttr;

    /** 商家性质：1民营 2国营 3外资 4合资 */
    private Integer merchantNature;

    /** 经营业态 */
    private String formatType;

    /** 自然人姓名 */
    private String naturalPerson;

    /** 身份证号（明文，Service层加密后存储） */
    private String idCard;

    /** 地址 */
    private String address;

    /** 手机 */
    private String phone;

    /** 商家评级：1优秀 2良好 3一般 4差 */
    private Integer merchantLevel;

    /** 审核状态：0待审核 1通过 2驳回 */
    private Integer auditStatus;

    /** 联系人列表 */
    private List<MerchantContactDTO> contacts;

    /** 开票信息列表 */
    private List<MerchantInvoiceDTO> invoices;
}
