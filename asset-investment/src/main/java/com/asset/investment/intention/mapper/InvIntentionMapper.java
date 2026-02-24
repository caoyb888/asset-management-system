package com.asset.investment.intention.mapper;

import com.asset.investment.intention.dto.IntentionQueryDTO;
import com.asset.investment.intention.entity.InvIntention;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 意向协议 Mapper */
@Mapper
public interface InvIntentionMapper extends BaseMapper<InvIntention> {

    /**
     * 多条件分页查询（含商铺联表过滤）
     * 利用 idx_intention_multi 复合索引加速主表扫描
     *
     * @param page  MBP 分页对象（自动注入 LIMIT/OFFSET）
     * @param query 查询条件
     */
    IPage<InvIntention> pageQueryWithCondition(IPage<InvIntention> page,
                                               @Param("q") IntentionQueryDTO query);
}
