package com.asset.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家列表/详情 VO
 */
@Data
public class MerchantVO {

    private Long id;
    private Long projectId;
    /** 项目名称（JOIN 查询） */
    private String projectName;

    private String merchantCode;
    private String merchantName;

    private Integer merchantAttr;
    /** 商家属性名称（Service 层填充） */
    private String merchantAttrName;

    private Integer merchantNature;
    /** 商家性质名称（Service 层填充） */
    private String merchantNatureName;

    private String formatType;
    private String naturalPerson;

    /** 身份证号（Service 层解密后返回，需脱敏） */
    private String idCard;

    private String address;
    private String phone;

    private Integer merchantLevel;
    /** 商家评级名称（Service 层填充） */
    private String merchantLevelName;

    private Integer auditStatus;
    /** 审核状态名称（Service 层填充） */
    private String auditStatusName;

    /** 联系人列表（详情接口返回） */
    private List<MerchantContactVO> contacts;

    /** 开票信息列表（详情接口返回） */
    private List<MerchantInvoiceVO> invoices;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
