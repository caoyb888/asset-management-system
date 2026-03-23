package com.asset.workflow.service.impl;

import com.asset.common.exception.BizException;
import com.asset.workflow.entity.WfProcessDefinition;
import com.asset.workflow.mapper.WfProcessDefinitionMapper;
import com.asset.workflow.service.WfProcessDefinitionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class WfProcessDefinitionServiceImpl
        extends ServiceImpl<WfProcessDefinitionMapper, WfProcessDefinition>
        implements WfProcessDefinitionService {

    @Override
    public WfProcessDefinition getByBusinessType(String businessType) {
        WfProcessDefinition def = getOne(new LambdaQueryWrapper<WfProcessDefinition>()
                .eq(WfProcessDefinition::getBusinessType, businessType)
                .eq(WfProcessDefinition::getIsEnabled, 1)
                .last("LIMIT 1"));
        if (def == null) {
            throw new BizException("未找到业务类型 [" + businessType + "] 的启用流程定义");
        }
        return def;
    }
}
