package com.asset.finance.dashboard.service.impl;

import com.asset.finance.dashboard.dto.DashboardSummaryVO;
import com.asset.finance.dashboard.dto.DashboardSummaryVO.NameValueVO;
import com.asset.finance.dashboard.dto.OverdueTopVO;
import com.asset.finance.dashboard.dto.ReceiptTrendVO;
import com.asset.finance.dashboard.service.FinDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务看板 ServiceImpl
 * <p>使用 JdbcTemplate 执行聚合查询，避免加载全量数据到内存</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinDashboardServiceImpl implements FinDashboardService {

    private final JdbcTemplate jdbcTemplate;

    /** 核销类型名称映射 */
    private static final Map<Integer, String> WRITE_OFF_TYPE_NAMES = new LinkedHashMap<>();

    static {
        WRITE_OFF_TYPE_NAMES.put(1, "收款核销");
        WRITE_OFF_TYPE_NAMES.put(2, "保证金核销");
        WRITE_OFF_TYPE_NAMES.put(3, "预收款核销");
        WRITE_OFF_TYPE_NAMES.put(4, "负数核销");
    }

    @Override
    public DashboardSummaryVO getSummary() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        DashboardSummaryVO vo = new DashboardSummaryVO();

        // ── 本月应收合计 ──────────────────────────────────────────────────────
        BigDecimal monthReceivable = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(actual_amount), 0) FROM fin_receivable " +
                "WHERE accrual_month = ? AND is_deleted = 0",
                BigDecimal.class, currentMonth);
        vo.setMonthReceivable(monthReceivable);

        // ── 本月已收合计 ──────────────────────────────────────────────────────
        BigDecimal monthReceived = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(received_amount), 0) FROM fin_receivable " +
                "WHERE accrual_month = ? AND is_deleted = 0",
                BigDecimal.class, currentMonth);
        vo.setMonthReceived(monthReceived);

        // ── 当前欠费合计（已逾期 status IN(0,1)）────────────────────────────
        BigDecimal currentOverdue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(outstanding_amount), 0) FROM fin_receivable " +
                "WHERE status IN (0, 1) AND due_date < CURDATE() AND is_deleted = 0",
                BigDecimal.class);
        vo.setCurrentOverdue(currentOverdue);

        // ── 本月核销笔数（已通过 status=1）──────────────────────────────────
        Long writeOffCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fin_write_off " +
                "WHERE status = 1 AND DATE_FORMAT(create_time, '%Y-%m') = ? AND is_deleted = 0",
                Long.class, currentMonth);
        vo.setMonthWriteOffCount(writeOffCount != null ? writeOffCount : 0L);

        // ── 应收费项分布（饼图）──────────────────────────────────────────────
        List<NameValueVO> feeDistribution = jdbcTemplate.query(
                "SELECT COALESCE(fee_name, '其他') AS name, SUM(actual_amount) AS val " +
                "FROM fin_receivable WHERE is_deleted = 0 " +
                "GROUP BY fee_name ORDER BY val DESC LIMIT 10",
                (rs, rowNum) -> new NameValueVO(
                        rs.getString("name"),
                        rs.getBigDecimal("val")));
        vo.setFeeTypeDistribution(feeDistribution);

        // ── 核销方式分布（饼图）──────────────────────────────────────────────
        List<NameValueVO> writeOffDistribution = jdbcTemplate.query(
                "SELECT write_off_type, COUNT(*) AS cnt " +
                "FROM fin_write_off WHERE is_deleted = 0 AND status = 1 " +
                "GROUP BY write_off_type",
                (rs, rowNum) -> {
                    Integer type = rs.getInt("write_off_type");
                    String name = WRITE_OFF_TYPE_NAMES.getOrDefault(type, "类型" + type);
                    return new NameValueVO(name, BigDecimal.valueOf(rs.getLong("cnt")));
                });
        vo.setWriteOffTypeDistribution(writeOffDistribution);

        return vo;
    }

    @Override
    public List<ReceiptTrendVO> getReceiptTrend() {
        // 查近12个月实收数据（status != 3 排除已作废收款单）
        String sql = "SELECT DATE_FORMAT(receipt_date, '%Y-%m') AS month, " +
                "       SUM(total_amount) AS amount " +
                "FROM fin_receipt " +
                "WHERE is_deleted = 0 AND status != 3 " +
                "  AND receipt_date >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 11 MONTH), '%Y-%m-01') " +
                "GROUP BY DATE_FORMAT(receipt_date, '%Y-%m') " +
                "ORDER BY month ASC";

        List<ReceiptTrendVO> dbResult = jdbcTemplate.query(sql,
                (rs, rowNum) -> new ReceiptTrendVO(
                        rs.getString("month"),
                        rs.getBigDecimal("amount")));

        // 补全近12个月，无数据月份填0
        return fillMissingMonths(dbResult);
    }

    @Override
    public List<OverdueTopVO> getOverdueTop() {
        // 按商家聚合欠费，取TOP10，merchant_name 冗余字段直接从应收表查
        String sql = "SELECT merchant_id, " +
                "       MIN(COALESCE(payer_name, CONCAT('商家', merchant_id))) AS merchant_name, " +
                "       SUM(outstanding_amount) AS overdue_amount " +
                "FROM fin_receivable " +
                "WHERE status IN (0, 1) AND due_date < CURDATE() " +
                "  AND is_deleted = 0 AND outstanding_amount > 0 " +
                "GROUP BY merchant_id " +
                "ORDER BY overdue_amount DESC " +
                "LIMIT 10";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OverdueTopVO item = new OverdueTopVO();
            item.setMerchantId(rs.getLong("merchant_id"));
            item.setMerchantName(rs.getString("merchant_name"));
            item.setOverdueAmount(rs.getBigDecimal("overdue_amount"));
            return item;
        });
    }

    /**
     * 补全近12个月的月份（无收款数据的月份填 0）
     */
    private List<ReceiptTrendVO> fillMissingMonths(List<ReceiptTrendVO> dbResult) {
        Map<String, BigDecimal> dbMap = dbResult.stream()
                .collect(Collectors.toMap(ReceiptTrendVO::getMonth, ReceiptTrendVO::getAmount));

        List<ReceiptTrendVO> result = new ArrayList<>(12);
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 11; i >= 0; i--) {
            String month = now.minusMonths(i).format(fmt);
            result.add(new ReceiptTrendVO(month, dbMap.getOrDefault(month, BigDecimal.ZERO)));
        }
        return result;
    }
}
