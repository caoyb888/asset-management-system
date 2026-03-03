package com.asset.report.common.permission;

import java.math.BigDecimal;

/**
 * 财务数据脱敏工具类
 * <p>
 * 根据当前线程的财务查看权限（{@link ReportPermissionContext#hasFinViewPerm()}）
 * 决定是否对财务绝对金额进行脱敏处理。
 * </p>
 *
 * <h3>脱敏规则</h3>
 * <ul>
 *   <li>管理员或拥有财务查看权限的用户：原值返回</li>
 *   <li>普通用户：绝对金额返回 {@code null}（前端显示为 "***"）</li>
 *   <li>同比/环比趋势数据（百分比）：始终返回原值</li>
 * </ul>
 */
public final class FinanceDataMaskUtil {

    private FinanceDataMaskUtil() {}

    /**
     * 对财务金额进行脱敏
     * <p>
     * 当前用户有财务查看权限时返回原值，否则返回 {@code null}。
     * </p>
     *
     * @param amount 原始金额
     * @return 脱敏后的金额（null 表示无权查看）
     */
    public static BigDecimal maskAmount(BigDecimal amount) {
        return ReportPermissionContext.hasFinViewPerm() ? amount : null;
    }

    /**
     * 判断当前用户是否需要脱敏（便于批量处理场景）
     *
     * @return true=需要脱敏，false=可见原值
     */
    public static boolean shouldMask() {
        return !ReportPermissionContext.hasFinViewPerm();
    }
}
