package com.asset.finance.common.adapter.impl;

import com.asset.finance.common.adapter.OaApprovalAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * OA 审批适配器 Mock 实现（开发/测试环境使用）
 *
 * <p>不依赖真实 OA 系统，所有调用均在本地模拟并打印日志。
 *
 * <p>切换至真实 OA 系统步骤：
 * <ol>
 *   <li>新建实现类（如 DingTalkOaApprovalAdapterImpl）实现 {@link OaApprovalAdapter}</li>
 *   <li>在新实现类上加 {@code @Primary} 注解，Spring 会优先注入</li>
 *   <li>本 Mock 实现保留，用于单元测试时通过 {@code @MockBean} 注入</li>
 * </ol>
 */
@Slf4j
@Component
public class OaApprovalAdapterImpl implements OaApprovalAdapter {

    @Override
    public String submitApproval(String businessType, Long businessId, String title) {
        // Mock：生成伪 approvalId = UUID前8位_businessId
        String mockApprovalId = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "_" + businessId;
        log.info("[OA-Mock] 提交审批 businessType={}, businessId={}, title={} → approvalId={}",
                businessType, businessId, title, mockApprovalId);
        /*
         * 真实钉钉审批示例：
         * DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/create");
         * OapiProcessinstanceCreateRequest req = new OapiProcessinstanceCreateRequest();
         * req.setProcessCode(businessType);
         * req.setOriginatorUserId(SecurityUtil.getCurrentUsername());
         * OapiProcessinstanceCreateResponse rsp = client.execute(req, accessToken);
         * return rsp.getProcessInstanceId();
         */
        return mockApprovalId;
    }

    @Override
    public Integer queryStatus(String approvalId) {
        // Mock：固定返回"待审批"
        log.debug("[OA-Mock] 查询审批状态 approvalId={} → 0(待审批)", approvalId);
        return 0;
    }

    @Override
    public void processCallback(String approvalId, Integer status, String comment) {
        // Mock：仅打印日志，不做业务处理
        // 真实回调由各业务模块的 Controller /callback 接口接收后调用对应 Service
        log.info("[OA-Mock] 收到审批回调 approvalId={}, status={}, comment={}",
                approvalId, status, comment);
    }
}
