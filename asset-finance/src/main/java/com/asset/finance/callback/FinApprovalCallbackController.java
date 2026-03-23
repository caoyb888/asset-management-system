package com.asset.finance.callback;

import com.asset.api.workflow.dto.ApprovalCallbackDTO;
import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.finance.deposit.service.FinDepositService;
import com.asset.finance.receipt.service.FinWriteOffService;
import com.asset.finance.receivable.service.FinReceivableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 统一审批回调入口 — workflow 服务回调财务模块
 * 路径: /fin/internal/approval-callback
 */
@Slf4j
@RestController
@RequestMapping("/fin/internal")
@RequiredArgsConstructor
public class FinApprovalCallbackController {

    private final FinWriteOffService writeOffService;
    private final FinReceivableService receivableService;
    private final FinDepositService depositService;

    @PostMapping("/approval-callback")
    public R<Void> onCallback(@RequestBody ApprovalCallbackDTO dto) {
        log.info("[财务回调] type={}, bizId={}, result={}", dto.getBusinessType(), dto.getBusinessId(), dto.getResult());
        boolean approved = dto.getResult() == 2;
        switch (dto.getBusinessType()) {
            case "FIN_WRITE_OFF" -> writeOffService.approveCallback(
                    dto.getProcessInstanceId(), approved, dto.getComment());
            case "FIN_DEDUCTION" -> receivableService.deductionCallback(
                    dto.getProcessInstanceId(), approved);
            case "FIN_ADJUSTMENT" -> receivableService.adjustmentCallback(
                    dto.getProcessInstanceId(), approved);
            case "FIN_DEPOSIT_OP" -> depositService.approveCallback(
                    dto.getProcessInstanceId(), approved);
            default -> throw new BizException("未知业务类型: " + dto.getBusinessType());
        }
        return R.ok();
    }
}
