package com.asset.workflow.callback;

import com.asset.api.workflow.client.FinanceCallbackClient;
import com.asset.api.workflow.client.InvestmentCallbackClient;
import com.asset.api.workflow.client.OperationCallbackClient;
import com.asset.api.workflow.dto.ApprovalCallbackDTO;
import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审批完成后，根据 businessType 回调对应的业务模块。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalCallbackDispatcher {

    private final InvestmentCallbackClient investmentClient;
    private final OperationCallbackClient operationClient;
    private final FinanceCallbackClient financeClient;

    public void dispatch(ApprovalCallbackDTO dto) {
        String type = dto.getBusinessType();
        log.info("[回调分发] type={}, bizId={}, result={}", type, dto.getBusinessId(), dto.getResult());

        R<Void> result;
        if (type.startsWith("INV_")) {
            result = investmentClient.onApprovalCallback(dto);
        } else if (type.startsWith("OPR_")) {
            result = operationClient.onApprovalCallback(dto);
        } else if (type.startsWith("FIN_")) {
            result = financeClient.onApprovalCallback(dto);
        } else {
            throw new BizException("未知的业务类型: " + type);
        }

        if (result.getCode() != 200) {
            log.error("[回调失败] type={}, bizId={}, msg={}", type, dto.getBusinessId(), result.getMsg());
        }
    }
}
