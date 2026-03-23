package com.asset.workflow.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程定义配置表 wf_process_definition
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_definition")
public class WfProcessDefinition extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程 key，与 BPMN process id 对应 */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 对应业务类型枚举 */
    private String businessType;

    /** BPMN 2.0 XML 定义 */
    private String bpmnXml;

    /** 审批人策略：ROLE/DEPT_LEADER/SPECIFIC_USER/INITIATOR_LEADER */
    private String approverStrategy;

    /** 策略参数 JSON */
    private String approverConfig;

    /** 是否启用: 0禁用 1启用 */
    private Integer isEnabled;

    /** 版本号 */
    @Version
    private Integer version;
}
