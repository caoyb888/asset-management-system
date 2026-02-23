package com.asset.base.model.vo;

import lombok.Data;

/**
 * 项目合同甲方信息 VO
 */
@Data
public class ProjectContractVO {

    private Long id;
    private Long projectId;
    private String partyAName;
    private String partyAAbbr;
    private String partyAAddress;
    private String partyAPhone;
    private String businessLicense;
    private String legalRepresentative;
    private String email;
}
