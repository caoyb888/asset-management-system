package com.asset.operation.callback;

import com.asset.api.workflow.dto.ApprovalCallbackDTO;
import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.operation.change.service.OprContractChangeService;
import com.asset.operation.termination.service.OprContractTerminationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 统一审批回调入口 — workflow 服务回调营运模块
 * 路径: /opr/internal/approval-callback
 */
@Slf4j
@RestController
@RequestMapping("/opr/internal")
@RequiredArgsConstructor
public class OprApprovalCallbackController {

    private final OprContractChangeService changeService;
    private final OprContractTerminationService terminationService;

    @PostMapping("/approval-callback")
    public R<Void> onCallback(@RequestBody ApprovalCallbackDTO dto) {
        log.info("[营运回调] type={}, bizId={}, result={}", dto.getBusinessType(), dto.getBusinessId(), dto.getResult());
        switch (dto.getBusinessType()) {
            case "OPR_CONTRACT_CHANGE" -> changeService.handleApprovalCallback(
                    dto.getBusinessId(), dto.getResult(), dto.getComment());
            case "OPR_TERMINATION" -> terminationService.handleApprovalCallback(
                    dto.getBusinessId(), dto.getResult(), dto.getComment());
            default -> throw new BizException("未知业务类型: " + dto.getBusinessType());
        }
        return R.ok();
    }
}
