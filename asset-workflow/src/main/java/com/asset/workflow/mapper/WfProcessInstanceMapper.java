package com.asset.workflow.mapper;

import com.asset.workflow.entity.WfProcessInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WfProcessInstanceMapper extends BaseMapper<WfProcessInstance> {

    /** 按业务类型+业务ID查询（加行锁） */
    @Select("SELECT * FROM wf_process_instance WHERE business_type = #{businessType} AND business_id = #{businessId} AND is_deleted = 0 FOR UPDATE")
    WfProcessInstance selectByBusinessForUpdate(@Param("businessType") String businessType,
                                                 @Param("businessId") Long businessId);
}
