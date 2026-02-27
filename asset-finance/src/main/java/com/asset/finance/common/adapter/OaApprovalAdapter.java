package com.asset.finance.common.adapter;

/**
 * 财务模块 OA 审批系统适配器接口
 *
 * <p>隔离财务业务代码与外部审批系统的耦合，支持切换 OA 实现（钉钉/泛微/本地Mock）。
 *
 * <p>财务模块审批业务类型约定：
 * <ul>
 *   <li>FIN_WRITE_OFF    — 核销单审批</li>
 *   <li>FIN_DEDUCTION    — 应收减免审批</li>
 *   <li>FIN_ADJUSTMENT   — 应收调整审批</li>
 *   <li>FIN_DEPOSIT_OP   — 保证金操作（冲抵/退款/罚没）审批</li>
 *   <li>FIN_PREPAY_OP    — 预收款操作（抵冲/退款）审批</li>
 * </ul>
 */
public interface OaApprovalAdapter {

    /**
     * 提交审批申请
     *
     * @param businessType 业务类型（见上方约定）
     * @param businessId   业务单据主键 ID
     * @param title        审批流程标题，如"核销单审批-WO-20260201-000001"
     * @return OA 系统返回的流程实例 ID（approvalId），存储至单据表用于回调匹配
     */
    String submitApproval(String businessType, Long businessId, String title);

    /**
     * 查询审批状态
     *
     * @param approvalId OA 流程实例 ID
     * @return 0-待审批 / 1-已通过 / 2-已驳回
     */
    Integer queryStatus(String approvalId);

    /**
     * 处理审批回调（OA 系统主动推送时调用）
     *
     * <p>实现类需保证幂等：同一 approvalId 重复调用只处理一次。
     *
     * @param approvalId OA 流程实例 ID
     * @param status     回调结果：1-通过 / 2-驳回
     * @param comment    审批意见
     */
    void processCallback(String approvalId, Integer status, String comment);
}
