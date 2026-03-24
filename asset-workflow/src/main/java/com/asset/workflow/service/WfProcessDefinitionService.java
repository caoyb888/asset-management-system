package com.asset.workflow.service;

import com.asset.workflow.dto.NodeConfigDTO;
import com.asset.workflow.dto.WfDefinitionSaveDTO;
import com.asset.workflow.entity.WfNodeConfig;
import com.asset.workflow.entity.WfProcessDefinition;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 流程定义管理服务
 */
public interface WfProcessDefinitionService extends IService<WfProcessDefinition> {

    /**
     * 根据业务类型获取启用的流程定义
     */
    WfProcessDefinition getByBusinessType(String businessType);

    /**
     * WD-02 新增/更新流程定义（支持可视化节点配置和 XML 源码两种模式）
     *
     * @return 保存后的流程定义 ID
     */
    Long saveDefinition(WfDefinitionSaveDTO dto);

    /**
     * WD-05 查询某流程定义的节点配置列表（供前端可视化设计器回显）
     */
    List<WfNodeConfig> getNodesByDefinitionId(Long definitionId);

    /**
     * WD-06 根据节点配置预览生成的 BPMN XML（不保存）
     */
    String previewBpmn(String processKey, String processName, List<NodeConfigDTO> nodeConfigs);

    /**
     * WD-07 手动触发流程重新部署到 Flowable 引擎
     * <p>
     * 适用场景：热部署失败后管理员手动重试，或引擎重启后强制同步最新 XML
     *
     * @param id 流程定义 ID
     * @return Flowable 部署 ID
     */
    String redeployById(Long id);
}
