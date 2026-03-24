package com.asset.workflow.service.impl;

import com.asset.common.model.R;
import com.asset.workflow.feign.SystemFeignClient;
import com.asset.workflow.service.ApproverResolveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 审批人自动解析服务实现
 * <p>
 * 通过 Feign 调用 asset-system 获取部门负责人 ID，填入流程变量。
 * 解析失败时仅记录警告日志，不阻断流程发起。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproverResolveServiceImpl implements ApproverResolveService {

    private final SystemFeignClient systemFeignClient;

    @Override
    public void resolveAndFill(Long initiatorId, Map<String, Object> variables) {
        if (initiatorId == null) {
            return;
        }

        try {
            R<Long> result = systemFeignClient.getDeptLeaderId(initiatorId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                Long leaderId = result.getData();
                // DEPT_LEADER 和 INITIATOR_LEADER 策略均解析为发起人所在部门的负责人
                variables.put("deptLeaderId", String.valueOf(leaderId));
                variables.put("initiatorLeaderId", String.valueOf(leaderId));
                log.debug("[审批人解析] initiatorId={}, deptLeaderId={}", initiatorId, leaderId);
            } else {
                log.warn("[审批人解析] 未获取到部门负责人，initiatorId={}, response={}",
                        initiatorId, result);
            }
        } catch (Exception e) {
            log.warn("[审批人解析] 调用 asset-system 失败，initiatorId={}, 原因={}（流程仍将发起，但 DEPT_LEADER/INITIATOR_LEADER 节点可能无人审批）",
                    initiatorId, e.getMessage());
        }
    }
}
