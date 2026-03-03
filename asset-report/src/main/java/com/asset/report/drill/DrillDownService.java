package com.asset.report.drill;

import com.asset.report.drill.dto.DrillDownRequestDTO;
import com.asset.report.drill.vo.DrillDownResultVO;

/**
 * 数据钻取 Service
 */
public interface DrillDownService {

    /**
     * 执行一次钻取，返回下一层级数据
     *
     * @param dto 钻取请求（reportCode + fromLevel + dimensionId + 时间过滤）
     * @return 下一层视图（columns + rows + 层级元信息）
     * @throws IllegalArgumentException 不支持的 reportCode 或已到叶子节点
     */
    DrillDownResultVO drillDown(DrillDownRequestDTO dto);
}
