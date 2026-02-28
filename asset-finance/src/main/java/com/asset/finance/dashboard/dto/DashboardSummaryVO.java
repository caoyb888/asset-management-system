package com.asset.finance.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务看板汇总 VO
 */
@Data
@Schema(description = "财务看板汇总数据")
public class DashboardSummaryVO {

    @Schema(description = "本月应收合计")
    private BigDecimal monthReceivable;

    @Schema(description = "本月已收合计")
    private BigDecimal monthReceived;

    @Schema(description = "当前欠费合计（已逾期应收）")
    private BigDecimal currentOverdue;

    @Schema(description = "本月核销笔数")
    private Long monthWriteOffCount;

    @Schema(description = "应收费项分布（饼图数据）")
    private List<NameValueVO> feeTypeDistribution;

    @Schema(description = "核销方式分布（饼图数据）")
    private List<NameValueVO> writeOffTypeDistribution;

    /** 饼图通用数据点 */
    @Data
    public static class NameValueVO {
        private String name;
        private BigDecimal value;

        public NameValueVO(String name, BigDecimal value) {
            this.name = name;
            this.value = value;
        }
    }
}
