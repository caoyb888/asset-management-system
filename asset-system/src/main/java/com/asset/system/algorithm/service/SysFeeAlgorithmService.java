package com.asset.system.algorithm.service;

import com.asset.system.algorithm.dto.CalcTestDTO;
import com.asset.system.algorithm.dto.CalcTestResultVO;
import com.asset.system.algorithm.dto.FeeAlgorithmCreateDTO;
import com.asset.system.algorithm.dto.FeeAlgorithmQueryDTO;
import com.asset.system.algorithm.entity.SysFeeAlgorithm;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 租费算法规则 Service */
public interface SysFeeAlgorithmService extends IService<SysFeeAlgorithm> {

    /** 分页查询 */
    IPage<SysFeeAlgorithm> pageQuery(FeeAlgorithmQueryDTO query);

    /** 查询启用算法列表（供业务模块下拉选择） */
    List<SysFeeAlgorithm> listEnabled();

    /** 新增算法 */
    Long createAlgorithm(FeeAlgorithmCreateDTO dto);

    /** 更新算法 */
    void updateAlgorithm(FeeAlgorithmCreateDTO dto);

    /** 删除算法 */
    void deleteAlgorithm(Long id);

    /** 启用/停用 */
    void changeStatus(Long id, Integer status);

    /**
     * 服务端试算
     * 解析变量、代入公式、用 SpEL 求值，返回结果和展开公式
     */
    CalcTestResultVO testCalc(CalcTestDTO dto);
}
