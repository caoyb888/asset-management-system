-- ============================================================
-- 报表模块测试数据填充脚本（跨库 ETL）
-- 目标库：asset_report
-- 来源库：asset_db
-- 统计基准日：2026-03-10
-- 统计月份：2026-03
-- 使用方式：
--   docker exec asset-mysql-tmp mysql -uroot -proot123 < sql/test-data-rpt.sql
-- ============================================================

SET @stat_date   = '2026-03-10';
SET @stat_month  = '2026-03';
-- 营运数据在2026-01（测试数据填报月份），单独运行
SET @opr_month   = '2026-01';

-- ============================================================
-- 1. rpt_asset_daily — 资产日汇总
--    来源：asset_db.biz_shop + inv_lease_contract_shop + inv_lease_contract
--    汇总粒度：项目+楼栋+楼层+业态（最细）/ 楼层汇总 / 楼栋汇总 / 项目汇总
-- ============================================================

DELETE FROM asset_report.rpt_asset_daily WHERE stat_date = @stat_date;

INSERT INTO asset_report.rpt_asset_daily
    (stat_date, project_id, building_id, floor_id, format_type,
     total_shops, rented_shops, vacant_shops, decorating_shops, opened_shops,
     total_area, rented_area, vacant_area, decoration_area,
     vacancy_rate, rental_rate, opening_rate)
/* ---- 项目+楼栋+楼层+业态 细粒度 ---- */
SELECT
    @stat_date,
    s.project_id,
    COALESCE(cs.building_id, 0)                                                AS building_id,
    COALESCE(cs.floor_id,   0)                                                 AS floor_id,
    COALESCE(s.planned_format, '')                                             AS format_type,
    COUNT(DISTINCT s.id)                                                       AS total_shops,
    COUNT(DISTINCT CASE WHEN s.shop_status = 1 THEN s.id END)                 AS rented_shops,
    COUNT(DISTINCT CASE WHEN s.shop_status = 0 THEN s.id END)                 AS vacant_shops,
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN s.id END)                                                         AS decorating_shops,
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.opening_date IS NOT NULL
             AND lc.opening_date <= @stat_date
        THEN s.id END)                                                         AS opened_shops,
    SUM(COALESCE(s.rent_area, 0))                                              AS total_area,
    SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END) AS rented_area,
    SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END) AS vacant_area,
    SUM(CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN COALESCE(cs.area, s.rent_area, 0)
        ELSE 0 END)                                                            AS decoration_area,
    /* vacancy_rate = vacant_area / total_area * 100 */
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2)
         ELSE 0.00 END,
    /* rental_rate = rented_area / total_area * 100 */
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2)
         ELSE 0.00 END,
    /* opening_rate = opened_shops / total_shops * 100 */
    CASE WHEN COUNT(DISTINCT s.id) > 0
         THEN ROUND(COUNT(DISTINCT CASE
                WHEN lc.status = 2 AND lc.opening_date IS NOT NULL
                     AND lc.opening_date <= @stat_date
                THEN s.id END) / COUNT(DISTINCT s.id) * 100, 2)
         ELSE 0.00 END
FROM asset_db.biz_shop s
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.shop_id = s.id AND cs.is_deleted = 0
LEFT JOIN asset_db.inv_lease_contract lc
       ON lc.id = cs.contract_id AND lc.status IN (2, 3) AND lc.is_deleted = 0
WHERE s.is_deleted = 0
GROUP BY s.project_id, COALESCE(cs.building_id, 0), COALESCE(cs.floor_id, 0), COALESCE(s.planned_format, '')

UNION ALL
/* ---- 项目+楼栋+楼层 汇总行 ---- */
SELECT
    @stat_date,
    s.project_id,
    COALESCE(cs.building_id, 0),
    COALESCE(cs.floor_id, 0),
    '',
    COUNT(DISTINCT s.id),
    COUNT(DISTINCT CASE WHEN s.shop_status = 1 THEN s.id END),
    COUNT(DISTINCT CASE WHEN s.shop_status = 0 THEN s.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN s.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.opening_date IS NOT NULL AND lc.opening_date <= @stat_date
        THEN s.id END),
    SUM(COALESCE(s.rent_area, 0)),
    SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END),
    SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END),
    SUM(CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN COALESCE(cs.area, s.rent_area, 0) ELSE 0 END),
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2) ELSE 0.00 END,
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2) ELSE 0.00 END,
    CASE WHEN COUNT(DISTINCT s.id) > 0
         THEN ROUND(COUNT(DISTINCT CASE
                WHEN lc.status = 2 AND lc.opening_date IS NOT NULL AND lc.opening_date <= @stat_date
                THEN s.id END) / COUNT(DISTINCT s.id) * 100, 2) ELSE 0.00 END
FROM asset_db.biz_shop s
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.shop_id = s.id AND cs.is_deleted = 0
LEFT JOIN asset_db.inv_lease_contract lc
       ON lc.id = cs.contract_id AND lc.status IN (2, 3) AND lc.is_deleted = 0
WHERE s.is_deleted = 0 AND cs.floor_id IS NOT NULL
GROUP BY s.project_id, COALESCE(cs.building_id, 0), COALESCE(cs.floor_id, 0)

UNION ALL
/* ---- 项目+楼栋 汇总行 ---- */
SELECT
    @stat_date,
    s.project_id,
    COALESCE(cs.building_id, 0),
    0,
    '',
    COUNT(DISTINCT s.id),
    COUNT(DISTINCT CASE WHEN s.shop_status = 1 THEN s.id END),
    COUNT(DISTINCT CASE WHEN s.shop_status = 0 THEN s.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN s.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.opening_date IS NOT NULL AND lc.opening_date <= @stat_date
        THEN s.id END),
    SUM(COALESCE(s.rent_area, 0)),
    SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END),
    SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END),
    SUM(CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN COALESCE(cs.area, s.rent_area, 0) ELSE 0 END),
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2) ELSE 0.00 END,
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2) ELSE 0.00 END,
    CASE WHEN COUNT(DISTINCT s.id) > 0
         THEN ROUND(COUNT(DISTINCT CASE
                WHEN lc.status = 2 AND lc.opening_date IS NOT NULL AND lc.opening_date <= @stat_date
                THEN s.id END) / COUNT(DISTINCT s.id) * 100, 2) ELSE 0.00 END
FROM asset_db.biz_shop s
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.shop_id = s.id AND cs.is_deleted = 0
LEFT JOIN asset_db.inv_lease_contract lc
       ON lc.id = cs.contract_id AND lc.status IN (2, 3) AND lc.is_deleted = 0
WHERE s.is_deleted = 0 AND cs.building_id IS NOT NULL
GROUP BY s.project_id, COALESCE(cs.building_id, 0)

UNION ALL
/* ---- 项目级汇总行 ---- */
SELECT
    @stat_date,
    s.project_id,
    0, 0, '',
    COUNT(DISTINCT s.id),
    COUNT(DISTINCT CASE WHEN s.shop_status = 1 THEN s.id END),
    COUNT(DISTINCT CASE WHEN s.shop_status = 0 THEN s.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN s.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND lc.opening_date IS NOT NULL AND lc.opening_date <= @stat_date
        THEN s.id END),
    SUM(COALESCE(s.rent_area, 0)),
    SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END),
    SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END),
    SUM(CASE
        WHEN lc.status = 2 AND lc.decoration_end IS NOT NULL AND lc.opening_date IS NULL
        THEN COALESCE(cs.area, s.rent_area, 0) ELSE 0 END),
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 0 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2) ELSE 0.00 END,
    CASE WHEN SUM(COALESCE(s.rent_area, 0)) > 0
         THEN ROUND(SUM(CASE WHEN s.shop_status = 1 THEN COALESCE(s.rent_area, 0) ELSE 0 END)
                    / SUM(COALESCE(s.rent_area, 0)) * 100, 2) ELSE 0.00 END,
    CASE WHEN COUNT(DISTINCT s.id) > 0
         THEN ROUND(COUNT(DISTINCT CASE
                WHEN lc.status = 2 AND lc.opening_date IS NOT NULL AND lc.opening_date <= @stat_date
                THEN s.id END) / COUNT(DISTINCT s.id) * 100, 2) ELSE 0.00 END
FROM asset_db.biz_shop s
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.shop_id = s.id AND cs.is_deleted = 0
LEFT JOIN asset_db.inv_lease_contract lc
       ON lc.id = cs.contract_id AND lc.status IN (2, 3) AND lc.is_deleted = 0
WHERE s.is_deleted = 0
GROUP BY s.project_id;

SELECT CONCAT('[1/5] rpt_asset_daily 写入 ', ROW_COUNT(), ' 条') AS result;

-- ============================================================
-- 2. rpt_investment_daily — 招商日汇总
--    来源：inv_lease_contract + inv_lease_contract_shop + inv_intention
-- ============================================================

DELETE FROM asset_report.rpt_investment_daily WHERE stat_date = @stat_date;

INSERT IGNORE INTO asset_report.rpt_investment_daily
    (stat_date, project_id, format_type, investment_manager_id,
     intention_count, intention_signed, new_intention,
     contract_count, contract_amount, contract_area, new_contract,
     conversion_rate, avg_rent_price)
/* ---- 项目+业态 细粒度 ---- */
SELECT
    @stat_date,
    lc.project_id,
    COALESCE(cs.format_type, '')                                               AS format_type,
    0                                                                          AS investment_manager_id,
    COUNT(DISTINCT CASE WHEN ii.status NOT IN (4, 5) THEN ii.id END)          AS intention_count,
    COUNT(DISTINCT CASE WHEN ii.status = 2 THEN ii.id END)                    AS intention_signed,
    COUNT(DISTINCT CASE
        WHEN ii.status NOT IN (4, 5) AND DATE(ii.created_at) = @stat_date
        THEN ii.id END)                                                        AS new_intention,
    COUNT(DISTINCT CASE WHEN lc.status = 2 THEN lc.id END)                    AS contract_count,
    SUM(CASE WHEN lc.status = 2 THEN COALESCE(lc.total_amount, 0) ELSE 0 END) AS contract_amount,
    SUM(CASE WHEN lc.status = 2 THEN COALESCE(cs.area, 0) ELSE 0 END)         AS contract_area,
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND DATE(lc.created_at) = @stat_date
        THEN lc.id END)                                                        AS new_contract,
    /* conversion_rate = contract_count / intention_count * 100 */
    CASE WHEN COUNT(DISTINCT CASE WHEN ii.status NOT IN (4, 5) THEN ii.id END) > 0
         THEN ROUND(COUNT(DISTINCT CASE WHEN lc.status = 2 THEN lc.id END)
                    / COUNT(DISTINCT CASE WHEN ii.status NOT IN (4, 5) THEN ii.id END) * 100, 2)
         ELSE 0.00 END,
    CASE WHEN SUM(CASE WHEN lc.status = 2 THEN COALESCE(cs.area, 0) ELSE 0 END) > 0
         THEN ROUND(SUM(CASE WHEN lc.status = 2 THEN COALESCE(lc.total_amount, 0) ELSE 0 END)
                    / SUM(CASE WHEN lc.status = 2 THEN COALESCE(cs.area, 0) ELSE 0 END), 2)
         ELSE 0.00 END
FROM asset_db.inv_lease_contract lc
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.contract_id = lc.id AND cs.is_deleted = 0
LEFT JOIN asset_db.inv_intention ii
       ON ii.id = lc.intention_id AND ii.is_deleted = 0
WHERE lc.is_deleted = 0
  AND lc.project_id IS NOT NULL
  AND lc.contract_start <= @stat_date
GROUP BY lc.project_id, COALESCE(cs.format_type, '')

UNION ALL
/* ---- 项目级汇总（全业态合计） ---- */
SELECT
    @stat_date,
    lc.project_id,
    '',
    0,
    COUNT(DISTINCT CASE WHEN ii.status NOT IN (4, 5) THEN ii.id END),
    COUNT(DISTINCT CASE WHEN ii.status = 2 THEN ii.id END),
    COUNT(DISTINCT CASE
        WHEN ii.status NOT IN (4, 5) AND DATE(ii.created_at) = @stat_date
        THEN ii.id END),
    COUNT(DISTINCT CASE WHEN lc.status = 2 THEN lc.id END),
    SUM(CASE WHEN lc.status = 2 THEN COALESCE(lc.total_amount, 0) ELSE 0 END),
    SUM(CASE WHEN lc.status = 2 THEN COALESCE(cs.area, 0) ELSE 0 END),
    COUNT(DISTINCT CASE
        WHEN lc.status = 2 AND DATE(lc.created_at) = @stat_date
        THEN lc.id END),
    CASE WHEN COUNT(DISTINCT CASE WHEN ii.status NOT IN (4, 5) THEN ii.id END) > 0
         THEN ROUND(COUNT(DISTINCT CASE WHEN lc.status = 2 THEN lc.id END)
                    / COUNT(DISTINCT CASE WHEN ii.status NOT IN (4, 5) THEN ii.id END) * 100, 2)
         ELSE 0.00 END,
    CASE WHEN SUM(CASE WHEN lc.status = 2 THEN COALESCE(cs.area, 0) ELSE 0 END) > 0
         THEN ROUND(SUM(CASE WHEN lc.status = 2 THEN COALESCE(lc.total_amount, 0) ELSE 0 END)
                    / SUM(CASE WHEN lc.status = 2 THEN COALESCE(cs.area, 0) ELSE 0 END), 2)
         ELSE 0.00 END
FROM asset_db.inv_lease_contract lc
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.contract_id = lc.id AND cs.is_deleted = 0
LEFT JOIN asset_db.inv_intention ii
       ON ii.id = lc.intention_id AND ii.is_deleted = 0
WHERE lc.is_deleted = 0
  AND lc.project_id IS NOT NULL
  AND lc.contract_start <= @stat_date
GROUP BY lc.project_id;

SELECT CONCAT('[2/5] rpt_investment_daily 写入 ', ROW_COUNT(), ' 条') AS result;

-- ============================================================
-- 3. rpt_finance_monthly — 财务月汇总
--    来源：fin_receivable + fin_deposit_account + fin_prepay_account
-- ============================================================

DELETE FROM asset_report.rpt_finance_monthly WHERE stat_month = @stat_month;

-- 3a. 项目+费项 细粒度
INSERT IGNORE INTO asset_report.rpt_finance_monthly
    (stat_month, project_id, fee_item_id, fee_item_type,
     receivable_amount, received_amount, outstanding_amount,
     overdue_amount, deduction_amount, adjustment_amount,
     overdue_rate, collection_rate, deposit_balance, prepay_balance)
SELECT
    @stat_month,
    r.project_id,
    COALESCE(r.fee_item_id, 0)                                                 AS fee_item_id,
    MAX(r.fee_name)                                                            AS fee_item_type,
    SUM(COALESCE(r.actual_amount, 0))                                          AS receivable_amount,
    SUM(COALESCE(r.received_amount, 0))                                        AS received_amount,
    SUM(COALESCE(r.outstanding_amount, 0))                                     AS outstanding_amount,
    SUM(CASE
        WHEN r.due_date IS NOT NULL
         AND r.due_date < DATE_ADD(DATE_FORMAT(@stat_month, '%Y-%m-01'), INTERVAL 1 MONTH)
         AND r.status IN (0, 1)
        THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END)                    AS overdue_amount,
    SUM(COALESCE(r.deduction_amount, 0))                                       AS deduction_amount,
    SUM(COALESCE(r.adjust_amount, 0))                                          AS adjustment_amount,
    /* overdue_rate = overdue_amount / receivable_amount * 100 */
    CASE WHEN SUM(COALESCE(r.actual_amount, 0)) > 0
         THEN ROUND(SUM(CASE
                WHEN r.due_date IS NOT NULL
                 AND r.due_date < DATE_ADD(DATE_FORMAT(@stat_month, '%Y-%m-01'), INTERVAL 1 MONTH)
                 AND r.status IN (0, 1)
                THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END)
                / SUM(COALESCE(r.actual_amount, 0)) * 100, 2)
         ELSE 0.00 END,
    /* collection_rate = received_amount / receivable_amount * 100 */
    CASE WHEN SUM(COALESCE(r.actual_amount, 0)) > 0
         THEN ROUND(SUM(COALESCE(r.received_amount, 0))
                    / SUM(COALESCE(r.actual_amount, 0)) * 100, 2)
         ELSE 0.00 END,
    0.00,   /* deposit_balance — updated below in STEP 3c */
    0.00    /* prepay_balance  — updated below in STEP 3d */
FROM asset_db.fin_receivable r
WHERE r.is_deleted = 0
  AND r.accrual_month = @stat_month
  AND r.project_id IS NOT NULL
GROUP BY r.project_id, COALESCE(r.fee_item_id, 0);

-- 3b. 项目级全费项汇总行（feeItemId=0）
INSERT IGNORE INTO asset_report.rpt_finance_monthly
    (stat_month, project_id, fee_item_id, fee_item_type,
     receivable_amount, received_amount, outstanding_amount,
     overdue_amount, deduction_amount, adjustment_amount,
     overdue_rate, collection_rate, deposit_balance, prepay_balance)
SELECT
    @stat_month,
    r.project_id,
    0,
    NULL,
    SUM(COALESCE(r.actual_amount, 0)),
    SUM(COALESCE(r.received_amount, 0)),
    SUM(COALESCE(r.outstanding_amount, 0)),
    SUM(CASE
        WHEN r.due_date IS NOT NULL
         AND r.due_date < DATE_ADD(DATE_FORMAT(@stat_month, '%Y-%m-01'), INTERVAL 1 MONTH)
         AND r.status IN (0, 1)
        THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(COALESCE(r.deduction_amount, 0)),
    SUM(COALESCE(r.adjust_amount, 0)),
    CASE WHEN SUM(COALESCE(r.actual_amount, 0)) > 0
         THEN ROUND(SUM(CASE
                WHEN r.due_date IS NOT NULL
                 AND r.due_date < DATE_ADD(DATE_FORMAT(@stat_month, '%Y-%m-01'), INTERVAL 1 MONTH)
                 AND r.status IN (0, 1)
                THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END)
                / SUM(COALESCE(r.actual_amount, 0)) * 100, 2) ELSE 0.00 END,
    CASE WHEN SUM(COALESCE(r.actual_amount, 0)) > 0
         THEN ROUND(SUM(COALESCE(r.received_amount, 0))
                    / SUM(COALESCE(r.actual_amount, 0)) * 100, 2) ELSE 0.00 END,
    0.00,
    0.00
FROM asset_db.fin_receivable r
WHERE r.is_deleted = 0
  AND r.accrual_month = @stat_month
  AND r.project_id IS NOT NULL
GROUP BY r.project_id;

-- 3c. 补充保证金余额（更新汇总行）
UPDATE asset_report.rpt_finance_monthly fm
INNER JOIN (
    SELECT da.project_id, SUM(da.balance) AS deposit_balance
    FROM asset_db.fin_deposit_account da
    WHERE da.is_deleted = 0 AND da.project_id IS NOT NULL
    GROUP BY da.project_id
) d ON d.project_id = fm.project_id
SET fm.deposit_balance = d.deposit_balance
WHERE fm.stat_month = @stat_month AND fm.fee_item_id = 0;

-- 3d. 补充预收款余额（更新汇总行）
UPDATE asset_report.rpt_finance_monthly fm
INNER JOIN (
    SELECT pa.project_id, SUM(pa.balance) AS prepay_balance
    FROM asset_db.fin_prepay_account pa
    WHERE pa.is_deleted = 0 AND pa.project_id IS NOT NULL
    GROUP BY pa.project_id
) p ON p.project_id = fm.project_id
SET fm.prepay_balance = p.prepay_balance
WHERE fm.stat_month = @stat_month AND fm.fee_item_id = 0;

SELECT CONCAT('[3/5] rpt_finance_monthly 写入完成，月份: ', @stat_month) AS result;

-- ============================================================
-- 4. rpt_operation_monthly — 营运月汇总
--    来源：opr_revenue_report + opr_floating_rent + opr_passenger_flow
--          + opr_contract_change + inv_lease_contract
-- ============================================================

DELETE FROM asset_report.rpt_operation_monthly WHERE stat_month = @opr_month;

INSERT IGNORE INTO asset_report.rpt_operation_monthly
    (stat_month, project_id, building_id, format_type,
     revenue_amount, floating_rent_amount, avg_revenue_per_sqm,
     passenger_flow, avg_daily_passenger,
     change_count, change_rent_impact,
     expiring_contracts, terminated_contracts)
/* ---- 项目+楼栋+业态 细粒度 ---- */
SELECT
    @opr_month,
    rr.project_id,
    COALESCE(cs.building_id, 0)                                                AS building_id,
    COALESCE(cs.format_type, '')                                               AS format_type,
    SUM(rr.revenue_amount)                                                     AS revenue_amount,
    COALESCE(SUM(fr.floating_rent), 0)                                         AS floating_rent_amount,
    /* avg_revenue_per_sqm = revenue / rented_area（此处简化使用营收/件数） */
    0.00                                                                       AS avg_revenue_per_sqm,
    COALESCE(SUM(pf.flow_count_sum), 0)                                        AS passenger_flow,
    /* avg_daily_passenger：按月天数简化 */
    CAST(COALESCE(SUM(pf.flow_count_sum), 0) / DAY(LAST_DAY(STR_TO_DATE(CONCAT(@opr_month, '-01'), '%Y-%m-%d'))) AS UNSIGNED),
    COUNT(DISTINCT CASE
        WHEN cc.effective_date >= DATE_FORMAT(@opr_month, '%Y-%m-01')
         AND cc.effective_date <  DATE_ADD(DATE_FORMAT(@opr_month, '%Y-%m-01'), INTERVAL 1 MONTH)
         AND cc.status = 2
        THEN cc.id END),
    0.00                                                                       AS change_rent_impact,
    COUNT(DISTINCT CASE
        WHEN lc.contract_end >= DATE_FORMAT(@opr_month, '%Y-%m-01')
         AND lc.contract_end <  DATE_ADD(DATE_FORMAT(@opr_month, '%Y-%m-01'), INTERVAL 1 MONTH)
        THEN lc.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status IN (3, 4)
         AND lc.updated_at >= DATE_FORMAT(@opr_month, '%Y-%m-01')
         AND lc.updated_at <  DATE_ADD(DATE_FORMAT(@opr_month, '%Y-%m-01'), INTERVAL 1 MONTH)
        THEN lc.id END)
FROM asset_db.opr_revenue_report rr
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.contract_id = rr.contract_id AND cs.is_deleted = 0
LEFT JOIN (
    SELECT contract_id, SUM(floating_rent) AS floating_rent
    FROM asset_db.opr_floating_rent
    WHERE calc_month = @opr_month AND is_deleted = 0
    GROUP BY contract_id
) fr ON fr.contract_id = rr.contract_id
LEFT JOIN (
    SELECT project_id, COALESCE(building_id, 0) AS building_id, SUM(flow_count) AS flow_count_sum
    FROM asset_db.opr_passenger_flow
    WHERE DATE_FORMAT(report_date, '%Y-%m') = @opr_month AND is_deleted = 0
    GROUP BY project_id, COALESCE(building_id, 0)
) pf ON pf.project_id = rr.project_id AND pf.building_id = COALESCE(cs.building_id, 0)
LEFT JOIN asset_db.opr_contract_change cc
       ON cc.project_id = rr.project_id AND cc.is_deleted = 0
LEFT JOIN asset_db.inv_lease_contract lc
       ON lc.id = rr.contract_id AND lc.is_deleted = 0
WHERE rr.is_deleted = 0
  AND rr.report_month = @opr_month
  AND rr.status = 1
GROUP BY rr.project_id, COALESCE(cs.building_id, 0), COALESCE(cs.format_type, '')

UNION ALL
/* ---- 项目级汇总（全楼栋、全业态） ---- */
SELECT
    @opr_month,
    rr.project_id,
    0,
    '',
    SUM(rr.revenue_amount),
    COALESCE(SUM(fr.floating_rent), 0),
    0.00,
    COALESCE(SUM(pf2.flow_count_sum), 0),
    CAST(COALESCE(SUM(pf2.flow_count_sum), 0) / DAY(LAST_DAY(STR_TO_DATE(CONCAT(@opr_month, '-01'), '%Y-%m-%d'))) AS UNSIGNED),
    COUNT(DISTINCT CASE
        WHEN cc.effective_date >= DATE_FORMAT(@opr_month, '%Y-%m-01')
         AND cc.effective_date <  DATE_ADD(DATE_FORMAT(@opr_month, '%Y-%m-01'), INTERVAL 1 MONTH)
         AND cc.status = 2
        THEN cc.id END),
    0.00,
    COUNT(DISTINCT CASE
        WHEN lc.contract_end >= DATE_FORMAT(@opr_month, '%Y-%m-01')
         AND lc.contract_end <  DATE_ADD(DATE_FORMAT(@opr_month, '%Y-%m-01'), INTERVAL 1 MONTH)
        THEN lc.id END),
    COUNT(DISTINCT CASE
        WHEN lc.status IN (3, 4)
         AND lc.updated_at >= DATE_FORMAT(@opr_month, '%Y-%m-01')
         AND lc.updated_at <  DATE_ADD(DATE_FORMAT(@opr_month, '%Y-%m-01'), INTERVAL 1 MONTH)
        THEN lc.id END)
FROM asset_db.opr_revenue_report rr
LEFT JOIN asset_db.inv_lease_contract_shop cs
       ON cs.contract_id = rr.contract_id AND cs.is_deleted = 0
LEFT JOIN (
    SELECT contract_id, SUM(floating_rent) AS floating_rent
    FROM asset_db.opr_floating_rent
    WHERE calc_month = @opr_month AND is_deleted = 0
    GROUP BY contract_id
) fr ON fr.contract_id = rr.contract_id
LEFT JOIN (
    SELECT project_id, SUM(flow_count) AS flow_count_sum
    FROM asset_db.opr_passenger_flow
    WHERE DATE_FORMAT(report_date, '%Y-%m') = @opr_month AND is_deleted = 0
    GROUP BY project_id
) pf2 ON pf2.project_id = rr.project_id
LEFT JOIN asset_db.opr_contract_change cc
       ON cc.project_id = rr.project_id AND cc.is_deleted = 0
LEFT JOIN asset_db.inv_lease_contract lc
       ON lc.id = rr.contract_id AND lc.is_deleted = 0
WHERE rr.is_deleted = 0
  AND rr.report_month = @opr_month
  AND rr.status = 1
GROUP BY rr.project_id;

SELECT CONCAT('[4/5] rpt_operation_monthly 写入 ', ROW_COUNT(), ' 条') AS result;

-- ============================================================
-- 5. rpt_aging_analysis — 账龄分析
--    来源：fin_receivable（仅统计逾期且有欠款的记录）
-- ============================================================

DELETE FROM asset_report.rpt_aging_analysis WHERE stat_date = @stat_date;

INSERT INTO asset_report.rpt_aging_analysis
    (stat_date, project_id, merchant_id, contract_id, fee_item_id,
     within_30, days_31_60, days_61_90, days_91_180, days_181_365, over_365,
     total_outstanding)
/* ---- 项目+商家+合同+费项 细粒度 ---- */
SELECT
    @stat_date,
    r.project_id,
    r.merchant_id,
    r.contract_id,
    COALESCE(r.fee_item_id, 0),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 1  AND 30  THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 31 AND 60  THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 61 AND 90  THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 91 AND 180 THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 181 AND 365 THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) > 365 THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(COALESCE(r.outstanding_amount, 0))                                     AS total_outstanding
FROM asset_db.fin_receivable r
WHERE r.is_deleted = 0
  AND r.outstanding_amount > 0
  AND r.due_date IS NOT NULL
  AND r.due_date < @stat_date
  AND r.status IN (0, 1)
  AND r.project_id IS NOT NULL
  AND r.merchant_id IS NOT NULL
  AND r.contract_id IS NOT NULL
GROUP BY r.project_id, r.merchant_id, r.contract_id, COALESCE(r.fee_item_id, 0)

UNION ALL
/* ---- 项目+商家+合同 汇总行（全费项合计，feeItemId=0） ---- */
SELECT
    @stat_date,
    r.project_id,
    r.merchant_id,
    r.contract_id,
    0,
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 1  AND 30  THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 31 AND 60  THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 61 AND 90  THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 91 AND 180 THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) BETWEEN 181 AND 365 THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(CASE WHEN DATEDIFF(@stat_date, r.due_date) > 365 THEN COALESCE(r.outstanding_amount, 0) ELSE 0 END),
    SUM(COALESCE(r.outstanding_amount, 0))
FROM asset_db.fin_receivable r
WHERE r.is_deleted = 0
  AND r.outstanding_amount > 0
  AND r.due_date IS NOT NULL
  AND r.due_date < @stat_date
  AND r.status IN (0, 1)
  AND r.project_id IS NOT NULL
  AND r.merchant_id IS NOT NULL
  AND r.contract_id IS NOT NULL
GROUP BY r.project_id, r.merchant_id, r.contract_id;

SELECT CONCAT('[5/5] rpt_aging_analysis 写入 ', ROW_COUNT(), ' 条') AS result;

-- ============================================================
-- 汇总校验
-- ============================================================
SELECT
    (SELECT COUNT(*) FROM asset_report.rpt_asset_daily      WHERE stat_date  = @stat_date)  AS asset_daily_rows,
    (SELECT COUNT(*) FROM asset_report.rpt_investment_daily WHERE stat_date  = @stat_date)  AS investment_daily_rows,
    (SELECT COUNT(*) FROM asset_report.rpt_finance_monthly  WHERE stat_month = @stat_month) AS finance_monthly_rows,
    (SELECT COUNT(*) FROM asset_report.rpt_operation_monthly WHERE stat_month = @opr_month) AS operation_monthly_rows,
    (SELECT COUNT(*) FROM asset_report.rpt_aging_analysis   WHERE stat_date  = @stat_date)  AS aging_analysis_rows;
