package com.asset.operation.change.dto;

import com.asset.operation.change.entity.OprContractChange;
import com.asset.operation.change.entity.OprContractChangeDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 合同变更详情 VO（含变更类型列表、字段明细） */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "合同变更详情")
public class ChangeDetailVO extends OprContractChange {

    @Schema(description = "变更类型编码列表")
    private List<String> changeTypeCodes;

    @Schema(description = "变更类型名称列表（展示用）")
    private List<String> changeTypeNames;

    @Schema(description = "字段级变更明细列表")
    private List<OprContractChangeDetail> details;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "合同编号")
    private String contractCode;

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "项目名称")
    private String projectName;
}
