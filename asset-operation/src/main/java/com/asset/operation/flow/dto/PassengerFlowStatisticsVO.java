package com.asset.operation.flow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/** 客流统计 VO（日/周环比 + 趋势数据） */
@Data
@Schema(description = "客流统计结果")
public class PassengerFlowStatisticsVO {

    @Schema(description = "今日客流")
    private Integer todayFlow;

    @Schema(description = "昨日客流")
    private Integer yesterdayFlow;

    @Schema(description = "日环比增幅（%，正数增长，负数下降）")
    private Double dayOverDayRate;

    @Schema(description = "本周合计")
    private Integer thisWeekFlow;

    @Schema(description = "上周合计")
    private Integer lastWeekFlow;

    @Schema(description = "周环比增幅（%）")
    private Double weekOverWeekRate;

    @Schema(description = "近30天总客流")
    private Integer last30DaysFlow;

    @Schema(description = "近30天每日趋势（按日期升序）")
    private List<DailyPoint> trendPoints;

    @Data
    @Schema(description = "趋势图数据点")
    public static class DailyPoint {
        @Schema(description = "日期（yyyy-MM-dd）")
        private String date;
        @Schema(description = "客流人数")
        private Integer flowCount;
    }
}
