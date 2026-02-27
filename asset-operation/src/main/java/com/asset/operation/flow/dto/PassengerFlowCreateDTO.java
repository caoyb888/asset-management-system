package com.asset.operation.flow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/** 新增/编辑客流填报 DTO */
@Data
@Schema(description = "新增/编辑客流填报参数")
public class PassengerFlowCreateDTO {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @Schema(description = "楼栋ID（项目整体录入时不传）")
    private Long buildingId;

    @Schema(description = "楼层ID（精确到楼层时传入）")
    private Long floorId;

    @Schema(description = "填报日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate reportDate;

    @Schema(description = "客流人数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer flowCount;

    @Schema(description = "数据来源（1手动/2导入/3设备），默认1")
    private Integer sourceType;
}
