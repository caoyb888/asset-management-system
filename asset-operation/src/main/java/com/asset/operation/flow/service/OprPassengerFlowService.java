package com.asset.operation.flow.service;

import com.asset.operation.flow.dto.PassengerFlowCreateDTO;
import com.asset.operation.flow.dto.PassengerFlowQueryDTO;
import com.asset.operation.flow.dto.PassengerFlowStatisticsVO;
import com.asset.operation.flow.entity.OprPassengerFlow;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 客流填报 Service 接口 */
public interface OprPassengerFlowService extends IService<OprPassengerFlow> {

    /** 分页查询 */
    IPage<OprPassengerFlow> pageQuery(PassengerFlowQueryDTO query);

    /** 新增填报（唯一键校验：project+building+floor+date） */
    Long create(PassengerFlowCreateDTO dto);

    /** 编辑填报（仅手动录入可改） */
    void update(Long id, PassengerFlowCreateDTO dto);

    /** 删除 */
    void delete(Long id);

    /**
     * 批量导入 Excel
     * @return 包含 successCount / failCount / errorList 的结果 Map
     */
    Map<String, Object> importExcel(MultipartFile file);

    /** 导出 Excel */
    void exportExcel(PassengerFlowQueryDTO query, HttpServletResponse response);

    /**
     * 统计分析（日/周环比 + 近30天趋势）
     * @param projectId 项目ID（必传），其余可选
     */
    PassengerFlowStatisticsVO statistics(Long projectId, Long buildingId, Long floorId);
}
