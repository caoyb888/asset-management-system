package com.asset.report.common.util;

import lombok.Data;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 异常值检测工具类（3σ 标准差法）
 * <p>
 * 在正态分布下，99.73% 的数据落在 [μ-3σ, μ+3σ] 范围内。
 * 超出该范围的值视为异常值（outlier），在报表中标红或告警。
 * </p>
 *
 * <h3>适用场景</h3>
 * <ul>
 *   <li>营收异常：某月营收明显偏高/偏低</li>
 *   <li>空置率异常：短期内空置率突变</li>
 *   <li>欠款异常：某商家欠款金额远超均值</li>
 *   <li>客流异常：某日客流量异常波动</li>
 * </ul>
 */
@UtilityClass
public class AnomalyDetectionUtil {

    private static final int SCALE = 4;
    /** 默认 σ 倍数（3σ 准则） */
    private static final double DEFAULT_SIGMA = 3.0;
    /** 最小样本量（样本 < MIN_SAMPLE 时不检测，直接标为正常） */
    private static final int MIN_SAMPLE = 3;

    // ==================== 检测结果类 ====================

    /**
     * 单个数据点的异常检测结果
     */
    @Data
    public static class AnomalyPoint {
        /** 数据点下标（对应输入列表的 index） */
        private final int index;
        /** 原始值 */
        private final BigDecimal value;
        /** Z-Score = (value - mean) / stdDev */
        private final double zScore;
        /** 是否为异常值 */
        private final boolean anomaly;
        /** 异常方向：HIGH=偏高, LOW=偏低, NORMAL=正常 */
        private final AnomalyDirection direction;
    }

    /** 异常方向枚举 */
    public enum AnomalyDirection {
        HIGH, LOW, NORMAL
    }

    /**
     * 异常检测汇总结果
     */
    @Data
    public static class AnomalyResult {
        /** 样本均值 */
        private final BigDecimal mean;
        /** 样本标准差 */
        private final BigDecimal stdDev;
        /** 异常上界 = mean + sigma * stdDev */
        private final BigDecimal upperBound;
        /** 异常下界 = mean - sigma * stdDev */
        private final BigDecimal lowerBound;
        /** 各数据点结果列表 */
        private final List<AnomalyPoint> points;
        /** 异常点总数 */
        private final int anomalyCount;
    }

    // ==================== 核心方法 ====================

    /**
     * 对数据序列执行 3σ 异常检测
     *
     * @param data 数值列表（按时间序列排列）
     * @return 检测结果（含均值、标准差、上下界、各点 Z-Score）
     */
    public AnomalyResult detect(List<BigDecimal> data) {
        return detect(data, DEFAULT_SIGMA);
    }

    /**
     * 对数据序列执行 N×σ 异常检测
     *
     * @param data  数值列表
     * @param sigma σ 倍数（越大则判定阈值越宽松，默认 3.0）
     * @return 检测结果
     */
    public AnomalyResult detect(List<BigDecimal> data, double sigma) {
        if (data == null || data.size() < MIN_SAMPLE) {
            // 样本不足，全部标为正常
            List<AnomalyPoint> points = buildNormalPoints(data);
            return new AnomalyResult(BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, points, 0);
        }

        // 过滤 null（视为 0）
        List<BigDecimal> clean = cleanData(data);

        BigDecimal mean   = calcMean(clean);
        BigDecimal stdDev = calcStdDev(clean, mean);

        BigDecimal sigmaDecimal = BigDecimal.valueOf(sigma);
        BigDecimal upperBound   = mean.add(sigmaDecimal.multiply(stdDev));
        BigDecimal lowerBound   = mean.subtract(sigmaDecimal.multiply(stdDev));

        List<AnomalyPoint> points = new ArrayList<>(data.size());
        int anomalyCount = 0;

        for (int i = 0; i < data.size(); i++) {
            BigDecimal val = data.get(i) != null ? data.get(i) : BigDecimal.ZERO;
            double zScore = stdDev.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : val.subtract(mean).doubleValue() / stdDev.doubleValue();

            boolean isAnomaly = Math.abs(zScore) > sigma;
            AnomalyDirection dir = AnomalyDirection.NORMAL;
            if (isAnomaly) {
                dir = zScore > 0 ? AnomalyDirection.HIGH : AnomalyDirection.LOW;
                anomalyCount++;
            }
            points.add(new AnomalyPoint(i, val,
                    roundDouble(zScore), isAnomaly, dir));
        }

        return new AnomalyResult(mean, stdDev, upperBound, lowerBound, points, anomalyCount);
    }

    /**
     * 仅返回异常点下标列表（轻量检测）
     *
     * @param data 数值列表
     * @return 异常点下标集合
     */
    public List<Integer> detectAnomalyIndices(List<BigDecimal> data) {
        return detectAnomalyIndices(data, DEFAULT_SIGMA);
    }

    /**
     * 仅返回异常点下标列表
     *
     * @param data  数值列表
     * @param sigma σ 倍数
     * @return 异常点下标集合
     */
    public List<Integer> detectAnomalyIndices(List<BigDecimal> data, double sigma) {
        AnomalyResult result = detect(data, sigma);
        List<Integer> indices = new ArrayList<>();
        for (AnomalyPoint p : result.getPoints()) {
            if (p.isAnomaly()) indices.add(p.getIndex());
        }
        return indices;
    }

    // ==================== 内部计算 ====================

    private List<BigDecimal> cleanData(List<BigDecimal> data) {
        List<BigDecimal> clean = new ArrayList<>(data.size());
        for (BigDecimal v : data) {
            clean.add(v != null ? v : BigDecimal.ZERO);
        }
        return clean;
    }

    private BigDecimal calcMean(List<BigDecimal> data) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : data) sum = sum.add(v);
        return sum.divide(BigDecimal.valueOf(data.size()), SCALE, RoundingMode.HALF_UP);
    }

    /** 样本标准差（Bessel 修正：除以 n-1） */
    private BigDecimal calcStdDev(List<BigDecimal> data, BigDecimal mean) {
        if (data.size() < 2) return BigDecimal.ZERO;
        double sum = 0.0;
        for (BigDecimal v : data) {
            double diff = v.subtract(mean).doubleValue();
            sum += diff * diff;
        }
        double variance = sum / (data.size() - 1);
        return BigDecimal.valueOf(Math.sqrt(variance))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private double roundDouble(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }

    private List<AnomalyPoint> buildNormalPoints(List<BigDecimal> data) {
        List<AnomalyPoint> points = new ArrayList<>();
        if (data == null) return points;
        for (int i = 0; i < data.size(); i++) {
            BigDecimal val = data.get(i) != null ? data.get(i) : BigDecimal.ZERO;
            points.add(new AnomalyPoint(i, val, 0.0, false, AnomalyDirection.NORMAL));
        }
        return points;
    }
}
