package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantQuery extends PageQuery {

    /** 所属项目ID */
    private Long projectId;

    /** 商家名称（模糊） */
    private String merchantName;

    /** 商家属性：1个体户 2企业 */
    private Integer merchantAttr;

    /** 商家性质：1民营 2国营 3外资 4合资 */
    private Integer merchantNature;

    /** 经营业态（模糊） */
    private String formatType;

    /** 商家评级：1优秀 2良好 3一般 4差 */
    private Integer merchantLevel;

    /** 审核状态：0待审核 1通过 2驳回 */
    private Integer auditStatus;
}
