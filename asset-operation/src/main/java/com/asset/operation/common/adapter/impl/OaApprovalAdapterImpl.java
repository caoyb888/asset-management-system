package com.asset.operation.common.adapter.impl;

import com.asset.operation.common.adapter.OaApprovalAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * OA 审批系统适配器 Mock 实现
 * <p>
 * 用于开发/测试阶段，不依赖真实 OA 系统
 * <p>
 * 真实 OA 集成步骤：
 * ① 新建实现类（如 DingTalkOaApprovalAdapterImpl）实现 {@link OaApprovalAdapter} 接口
 * ② {@link #submitApproval}：调用 OA REST API 提交工作流，入参映射到对应 OA 流程定义Key；
 *    返回 OA 系统生成的流程实例ID（instanceId）
 * ③ {@link #queryStatus}：调用 OA 查询接口，将 OA 状态码映射为本系统约定（0待审/1通过/2驳回）
 * ④ {@link #processCallback}：解析 OA 异步回调请求，验证签名，更新对应业务单据状态
 * ⑤ 在 Spring 配置中通过 @Primary 或条件注入切换至真实实现
 */
@Slf4j
@Component
public class OaApprovalAdapterImpl implements OaApprovalAdapter {

    @Override
    public String submitApproval(String businessType, Long businessId, String title) {
        // Mock：生成伪 approvalId（UUID前8位 + 业务ID）
        String mockApprovalId = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "_" + businessId;
        log.info("[OA适配器-Mock] 模拟提交审批，businessType={}，businessId={}，title={}，approvalId={}",
                businessType, businessId, title, mockApprovalId);
        // TODO: 替换为真实 OA 接口调用，示例（钉钉审批）：
        // DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/create");
        // OapiProcessinstanceCreateRequest req = new OapiProcessinstanceCreateRequest();
        // req.setProcessCode(businessType);
        // req.setOriginatorUserId(SecurityUtil.getCurrentUserId());
        // OapiProcessinstanceCreateResponse rsp = client.execute(req, accessToken);
        // return rsp.getProcessInstanceId();
        return mockApprovalId;
    }

    @Override
    public Integer queryStatus(String approvalId) {
        // Mock：固定返回"待审批"状态
        log.debug("[OA适配器-Mock] 查询审批状态，approvalId={}，mock返回：0(待审批)", approvalId);
        // TODO: 替换为 OA 系统查询接口调用，示例：
        // OaStatusResponse resp = oaClient.get("/workflow/instance/" + approvalId);
        // return mapOaStatus(resp.getStatus()); // OA状态码 → 本系统状态（0/1/2）
        return 0;
    }

    @Override
    public void processCallback(String approvalId, Integer status, String comment) {
        log.info("[OA适配器-Mock] 收到审批回调（mock处理），approvalId={}，status={}，comment={}",
                approvalId, status, comment);
        // 真实集成时，此处根据 approvalId 路由到对应业务模块的 Controller 回调接口
        // 生产环境示例：
        // String businessType = oaCallbackRouter.getBusinessType(approvalId);
        // Long businessId = oaCallbackRouter.getBusinessId(approvalId);
        // switch(businessType) {
        //   case "CONTRACT_CHANGE" -> contractChangeService.approvalCallback(businessId, status, comment);
        //   case "CONTRACT_TERMINATION" -> terminationService.approvalCallback(businessId, status, comment);
        // }
    }
}
