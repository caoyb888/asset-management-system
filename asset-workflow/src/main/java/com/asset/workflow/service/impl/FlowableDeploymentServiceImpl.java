package com.asset.workflow.service.impl;

import com.asset.common.exception.BizException;
import com.asset.workflow.service.FlowableDeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Flowable 流程热部署服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowableDeploymentServiceImpl implements FlowableDeploymentService {

    private final RepositoryService repositoryService;

    @Override
    public String deploy(String processKey, String bpmnXml) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new BizException("BPMN XML 不能为空，无法部署流程: " + processKey);
        }

        String resourceName = processKey.toLowerCase() + ".bpmn20.xml";

        try {
            Deployment deployment = repositoryService.createDeployment()
                    .name(processKey)
                    .addBytes(resourceName, bpmnXml.getBytes(StandardCharsets.UTF_8))
                    .deploy();

            log.info("[Flowable 热部署] processKey={}, deploymentId={}, resourceName={}",
                    processKey, deployment.getId(), resourceName);
            return deployment.getId();

        } catch (Exception e) {
            log.error("[Flowable 热部署失败] processKey={}, error={}", processKey, e.getMessage(), e);
            throw new BizException("流程部署失败: " + e.getMessage());
        }
    }

    @Override
    public int getLatestVersion(String processKey) {
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
        return def != null ? def.getVersion() : 0;
    }
}
