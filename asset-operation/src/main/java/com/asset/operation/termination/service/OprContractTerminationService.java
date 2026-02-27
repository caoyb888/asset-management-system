package com.asset.operation.termination.service;

import com.asset.operation.change.dto.ApprovalCallbackDTO;
import com.asset.operation.termination.dto.TerminationCreateDTO;
import com.asset.operation.termination.dto.TerminationDetailVO;
import com.asset.operation.termination.dto.TerminationQueryDTO;
import com.asset.operation.termination.entity.OprContractTermination;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** 合同解约 Service 接口 */
public interface OprContractTerminationService extends IService<OprContractTermination> {

    /** 分页查询解约列表 */
    IPage<TerminationDetailVO> pageQuery(TerminationQueryDTO query);

    /** 查询解约单详情（含清算明细） */
    TerminationDetailVO getDetailById(Long id);

    /** 新增解约单（草稿） */
    Long create(TerminationCreateDTO dto);

    /** 编辑解约单（仅草稿/驳回可改） */
    void update(Long id, TerminationCreateDTO dto);

    /** 触发清算引擎计算清算金额 */
    void calculateSettlement(Long id);

    /** 提交OA审批 */
    void submitApproval(Long id);

    /** 审批回调（通过→执行解约/驳回→回退草稿） */
    void onApprovalCallback(Long id, ApprovalCallbackDTO dto);
}
