package com.asset.workflow.service;

import com.asset.workflow.entity.WfProcessDefinition;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 流程定义管理服务
 */
public interface WfProcessDefinitionService extends IService<WfProcessDefinition> {

    /**
     * 根据业务类型获取启用的流程定义
     */
    WfProcessDefinition getByBusinessType(String businessType);
}
