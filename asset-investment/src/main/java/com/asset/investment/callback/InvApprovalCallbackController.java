package com.asset.investment.callback;

import com.asset.api.workflow.dto.ApprovalCallbackDTO;
import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.investment.intention.service.InvIntentionService;
import com.asset.investment.opening.service.InvOpeningApprovalService;
import com.asset.investment.decomposition.service.InvRentDecompositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 统一审批回调入口 — workflow 服务回调招商模块
 * 路径: /inv/internal/approval-callback
 */
@Slf4j
@RestController
@RequestMapping("/inv/internal")
@RequiredArgsConstructor
public class InvApprovalCallbackController {

    private final InvIntentionService intentionService;
    private final InvOpeningApprovalService openingService;
    private final InvRentDecompositionService decompositionService;

    @PostMapping("/approval-callback")
    public R<Void> onCallback(@RequestBody ApprovalCallbackDTO dto) {
        log.info("[招商回调] type={}, bizId={}, result={}", dto.getBusinessType(), dto.getBusinessId(), dto.getResult());
        switch (dto.getBusinessType()) {
            case "INV_INTENTION" -> intentionService.handleApprovalCallback(
                    dto.getBusinessId(), dto.getResult(), dto.getComment());
            case "INV_OPENING" -> openingService.handleApprovalCallback(
                    dto.getBusinessId(), dto.getResult(), dto.getComment());
            case "INV_RENT_DECOMP" -> decompositionService.handleApprovalCallback(
                    dto.getBusinessId(), dto.getResult(), dto.getComment());
            default -> throw new BizException("未知业务类型: " + dto.getBusinessType());
        }
        return R.ok();
    }
}
