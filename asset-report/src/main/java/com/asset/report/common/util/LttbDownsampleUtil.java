package com.asset.report.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * LTTB（Largest-Triangle-Three-Buckets）时序数据降采样工具
 * <p>
 * 当趋势图数据点超过阈值时自动降采样，保留视觉曲线形态（比等间距采样更接近原始曲线）。
 * 算法复杂度：O(n)，阈值以下原样返回不做处理。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * List&lt;RateTrendVO&gt; sampled = LttbDownsampleUtil.downsample(
 *     rawList, 1000, v -&gt; v.getValue() == null ? 0.0 : v.getValue().doubleValue());
 * </pre>
 */
public final class LttbDownsampleUtil {

    private LttbDownsampleUtil() {}

    /**
     * 对时序列表进行 LTTB 降采样
     *
     * @param data       原始数据（按时间顺序，不能为 null）
     * @param threshold  最大保留数据点数（≥ 3）；数据量 ≤ threshold 时原样返回
     * @param yExtractor 提取用于三角面积计算的 y 值（主要指标值）
     * @param <T>        数据点类型
     * @return 降采样后的列表（首尾点始终保留）
     */
    public static <T> List<T> downsample(List<T> data, int threshold, ToDoubleFunction<T> yExtractor) {
        if (data == null || data.size() <= threshold || threshold < 3) {
            return data;
        }

        int size = data.size();
        List<T> sampled = new ArrayList<>(threshold);

        // 始终保留第一个点
        sampled.add(data.get(0));

        double bucketSize = (double) (size - 2) / (threshold - 2);
        int a = 0; // 当前选中点的索引

        for (int i = 0; i < threshold - 2; i++) {
            // 下一个桶的范围（用于计算平均值，代表"将来"的趋势方向）
            int avgRangeStart = (int) Math.floor((i + 1) * bucketSize) + 1;
            int avgRangeEnd   = (int) Math.floor((i + 2) * bucketSize) + 1;
            if (avgRangeEnd > size) avgRangeEnd = size;

            // 计算下一个桶的均值点
            double avgY = 0;
            int avgLen = avgRangeEnd - avgRangeStart;
            for (int j = avgRangeStart; j < avgRangeEnd; j++) {
                avgY += yExtractor.applyAsDouble(data.get(j));
            }
            avgY /= avgLen;
            double avgX = (avgRangeStart + avgRangeEnd - 1) / 2.0;

            // 当前桶的范围
            int rangeStart = (int) Math.floor(i * bucketSize) + 1;
            int rangeEnd   = (int) Math.floor((i + 1) * bucketSize) + 1;
            if (rangeEnd > size) rangeEnd = size;

            // 找使三角形面积最大的点（a → 候选点 → 均值点）
            double pointAx = a;
            double pointAy = yExtractor.applyAsDouble(data.get(a));
            double maxArea = -1;
            int maxIdx = rangeStart;

            for (int j = rangeStart; j < rangeEnd; j++) {
                double area = Math.abs(
                    (pointAx - avgX) * (yExtractor.applyAsDouble(data.get(j)) - pointAy)
                    - (pointAx - j)  * (avgY - pointAy)
                ) * 0.5;
                if (area > maxArea) {
                    maxArea = area;
                    maxIdx  = j;
                }
            }

            sampled.add(data.get(maxIdx));
            a = maxIdx;
        }

        // 始终保留最后一个点
        sampled.add(data.get(size - 1));
        return sampled;
    }
}
