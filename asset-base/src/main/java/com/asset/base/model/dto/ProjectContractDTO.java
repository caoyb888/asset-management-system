package com.asset.base.model.dto;

import lombok.Data;

/**
 * 项目合同甲方信息 DTO
 */
@Data
public class ProjectContractDTO {

    /** 记录ID（更新时传入，新增时为空） */
    private Long id;

    /** 甲方名称 */
    private String partyAName;

    /** 甲方简称 */
    private String partyAAbbr;

    /** 甲方地址 */
    private String partyAAddress;

    /** 甲方电话 */
    private String partyAPhone;

    /** 营业执照号 */
    private String businessLicense;

    /** 法定代表人 */
    private String legalRepresentative;

    /** 邮箱 */
    private String email;
}
