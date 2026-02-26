package com.asset.operation.change.service;

import com.asset.operation.change.dto.*;
import com.asset.operation.change.entity.OprContractChange;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 合同变更 Service 接口 */
public interface OprContractChangeService extends IService<OprContractChange> {

    /** 分页查询变更单列表 */
    IPage<OprContractChange> pageQuery(ChangeQueryDTO query);

    /** 变更单详情（含变更类型/字段明细/关联信息） */
    ChangeDetailVO getDetailById(Long id);

    /** 新增变更单（草稿状态） */
    Long create(ChangeCreateDTO dto);

    /** 编辑变更单（仅草稿/驳回状态可修改） */
    void update(Long id, ChangeCreateDTO dto);

    /** 预览变更影响（受影响应收笔数/金额差异），结果暂存至 impact_summary */
    ChangeImpactVO previewImpact(Long changeId);

    /** 提交 OA 审批 */
    void submitApproval(Long changeId);

    /** 审批回调（通过后触发应收重算，驳回则状态回退） */
    void onApprovalCallback(Long changeId, ApprovalCallbackDTO dto);

    /** 查询合同变更历史时间线（按合同ID倒序） */
    List<ChangeDetailVO> listHistory(Long contractId);
}
