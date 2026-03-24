package com.asset.workflow.mapper;

import com.asset.workflow.entity.WfNodeConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程节点配置 Mapper
 */
@Mapper
public interface WfNodeConfigMapper extends BaseMapper<WfNodeConfig> {

    /**
     * 按流程定义 ID 查询节点列表，按 node_order 升序排列
     */
    List<WfNodeConfig> selectByDefinitionId(@Param("definitionId") Long definitionId);

    /**
     * 按流程定义 ID 逻辑删除所有节点（保存时先清后插）
     */
    void deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
