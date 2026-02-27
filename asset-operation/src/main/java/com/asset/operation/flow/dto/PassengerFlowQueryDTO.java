package com.asset.operation.flow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/** 客流填报分页查询参数 */
@Data
@Schema(description = "客流填报查询参数")
public class PassengerFlowQueryDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "楼栋ID")
    private Long buildingId;

    @Schema(description = "楼层ID")
    private Long floorId;

    @Schema(description = "填报日期起（含）")
    private LocalDate startDate;

    @Schema(description = "填报日期止（含）")
    private LocalDate endDate;

    @Schema(description = "数据来源（1手动/2导入/3设备）")
    private Integer sourceType;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum;

    @Schema(description = "每页大小", defaultValue = "20")
    private Integer pageSize;
}
