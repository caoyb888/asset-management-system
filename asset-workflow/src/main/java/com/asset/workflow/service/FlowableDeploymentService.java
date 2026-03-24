package com.asset.workflow.service;

/**
 * Flowable 流程热部署服务
 * <p>
 * 将 BPMN XML 部署到 Flowable 引擎，支持同一 processKey 的版本升级（热更新）。
 * 已运行的流程实例继续使用旧版本定义，新提交的审批使用最新版本。
 */
public interface FlowableDeploymentService {

    /**
     * 部署或更新流程定义
     *
     * @param processKey 流程 key（用作部署名称和资源文件名）
     * @param bpmnXml    BPMN 2.0 XML 字符串
     * @return Flowable 部署 ID
     * @throws com.asset.common.exception.BizException 当 BPMN XML 解析失败时
     */
    String deploy(String processKey, String bpmnXml);

    /**
     * 查询指定 processKey 在 Flowable 中的最新部署版本号
     *
     * @return 版本号（未部署时返回 0）
     */
    int getLatestVersion(String processKey);
}
