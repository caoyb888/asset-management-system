package com.asset.workflow.service;

import java.util.Map;

/**
 * 审批人自动解析服务
 * <p>
 * 在流程启动前，根据发起人信息解析出各审批节点所需的动态审批人变量，
 * 供 Flowable 流程引擎的 EL 表达式（如 ${deptLeaderId}）使用。
 */
public interface ApproverResolveService {

    /**
     * 根据发起人 userId 解析审批人变量，并追加到传入的 variables 中。
     * <p>
     * 解析结果：
     * <ul>
     *   <li>{@code deptLeaderId}  — 发起人所在部门的负责人用户ID</li>
     *   <li>{@code initiatorLeaderId} — 同上（INITIATOR_LEADER 策略使用）</li>
     * </ul>
     * 若解析失败（用户不存在、部门未配置负责人）则跳过对应变量，不抛异常。
     *
     * @param initiatorId 发起人用户ID
     * @param variables   待追加的流程变量 Map（可写入）
     */
    void resolveAndFill(Long initiatorId, Map<String, Object> variables);
}
