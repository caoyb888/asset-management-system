package com.asset.operation.termination.dto;

import com.asset.operation.termination.entity.OprTerminationSettlement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 解约单详情 VO */
@Data
@Schema(description = "解约单详情")
public class TerminationDetailVO {

    private Long id;
    private String terminationCode;
    private Long contractId;
    private Long ledgerId;
    private Long projectId;
    private Long merchantId;
    private Long brandId;
    private Long shopId;
    private Integer terminationType;
    private LocalDate terminationDate;
    private String reason;
    private Long newContractId;
    private BigDecimal penaltyAmount;
    private BigDecimal refundDeposit;
    private BigDecimal unsettledAmount;
    private BigDecimal settlementAmount;
    private Integer status;
    private String approvalId;

    // 冗余展示字段
    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "解约类型名称")
    private String terminationTypeName;

    @Schema(description = "合同编号")
    private String contractCode;

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "商铺编号")
    private String shopCode;

    @Schema(description = "清算明细列表")
    private List<OprTerminationSettlement> settlements;
}
