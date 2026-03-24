package com.asset.workflow.feign;

import com.asset.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 系统服务 Feign 客户端（供工作流模块内部调用）
 */
@FeignClient(name = "asset-system", contextId = "systemFeignClient")
public interface SystemFeignClient {

    /**
     * 获取指定用户所在部门的负责人用户ID
     */
    @GetMapping("/sys/users/{userId}/dept-leader-id")
    R<Long> getDeptLeaderId(@PathVariable("userId") Long userId);
}
