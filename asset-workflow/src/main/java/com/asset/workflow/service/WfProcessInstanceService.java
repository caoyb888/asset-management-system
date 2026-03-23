package com.asset.workflow.service;

import com.asset.api.workflow.dto.ProcessInstanceVO;
import com.asset.api.workflow.dto.ProcessPageQuery;
import com.asset.workflow.entity.WfProcessInstance;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 流程实例服务
 */
public interface WfProcessInstanceService extends IService<WfProcessInstance> {

    /** 按业务单据查流程实例 */
    WfProcessInstance getByBusiness(String businessType, Long businessId);

    /** 管理员分页查询 */
    IPage<ProcessInstanceVO> pageQuery(ProcessPageQuery query);

    /** 审批效率统计 */
    Map<String, Object> statistics();
}
