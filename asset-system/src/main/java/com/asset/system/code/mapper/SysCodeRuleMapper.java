package com.asset.system.code.mapper;

import com.asset.system.code.entity.SysCodeRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysCodeRuleMapper extends BaseMapper<SysCodeRule> {

    /** 悲观锁获取规则行，防止并发重复序号 */
    @Select("SELECT * FROM sys_code_rule WHERE rule_key = #{ruleKey} AND is_deleted = 0 FOR UPDATE")
    SysCodeRule selectByKeyForUpdate(String ruleKey);

    /** 更新序列号和当前周期 */
    @Update("UPDATE sys_code_rule SET current_seq = #{seq}, current_period = #{period} WHERE id = #{id}")
    int updateSeq(Long id, Long seq, String period);
}
