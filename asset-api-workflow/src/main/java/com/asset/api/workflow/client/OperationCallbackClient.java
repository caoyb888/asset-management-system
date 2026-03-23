package com.asset.api.workflow.client;

import com.asset.api.workflow.dto.ApprovalCallbackDTO;
import com.asset.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 工作流回调营运模块 Feign 接口
 */
@FeignClient(name = "asset-operation", path = "/opr/internal")
public interface OperationCallbackClient {

    @PostMapping("/approval-callback")
    R<Void> onApprovalCallback(@RequestBody ApprovalCallbackDTO dto);
}
