package com.asset.operation.ledger.dto;

import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 合同台账详情 VO（含应收计划列表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "合同台账详情")
public class LedgerDetailVO extends OprContractLedger {

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "合同编号")
    private String contractCode;

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "商铺编号（第一个商铺）")
    private String shopCode;

    @Schema(description = "商铺ID")
    private Long shopId;

    @Schema(description = "应收计划列表")
    private List<OprReceivablePlan> receivablePlans;

    @Schema(description = "合同类型名称")
    private String contractTypeName;

    @Schema(description = "双签状态名称")
    private String doubleSignStatusName;

    @Schema(description = "应收状态名称")
    private String receivableStatusName;

    @Schema(description = "审核状态名称")
    private String auditStatusName;

    @Schema(description = "台账状态名称")
    private String statusName;
}
