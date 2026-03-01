package com.asset.system.code.service;

import com.asset.system.code.dto.CodeRuleCreateDTO;
import com.asset.system.code.dto.CodeRuleQueryDTO;
import com.asset.system.code.entity.SysCodeRule;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** 业务编码规则 Service */
public interface SysCodeRuleService extends IService<SysCodeRule> {

    /** 分页查询 */
    IPage<SysCodeRule> pageQuery(CodeRuleQueryDTO query);

    /** 新增规则 */
    Long createRule(CodeRuleCreateDTO dto);

    /** 更新规则（不允许更改 ruleKey） */
    void updateRule(CodeRuleCreateDTO dto);

    /** 删除规则 */
    void deleteRule(Long id);

    /** 启用/停用 */
    void changeStatus(Long id, Integer status);

    /**
     * 生成下一个业务编码（事务性，SELECT FOR UPDATE）
     * 格式：{prefix}{sep}{dateStr}{sep}{seqPadded}
     * 若 prefix 为空则省略前缀段，dateFormat 为空则省略日期段
     *
     * @param ruleKey 规则标识键
     * @return 生成的编码字符串
     */
    String generateCode(String ruleKey);

    /** 手动重置序列号归零 */
    void resetSeq(Long id);
}
