package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 合同到期预警 VO
 * <p>
 * 按项目统计即将到期的合同数，支持 30/60/90 天分档预警。
 * 数据来源：{@code rpt_operation_monthly.expiring_contracts}（90天内）
 * 精确到期天数分档需实时查询 {@code inv_lease_contract}。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprExpiringContractVO {

    /** 项目ID */
    private Long projectId;

    /** 统计月份（yyyy-MM） */
    private String statMonth;

    /** 30天内即将到期合同数 */
    private Integer expiringWithin30;

    /** 31-60天即将到期合同数 */
    private Integer expiringWithin60;

    /** 61-90天即将到期合同数 */
    private Integer expiringWithin90;

    /** 90天内合计到期合同数 */
    private Integer totalExpiring;
}
