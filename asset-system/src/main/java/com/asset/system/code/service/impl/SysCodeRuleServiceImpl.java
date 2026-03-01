package com.asset.system.code.service.impl;

import com.asset.system.code.dto.CodeRuleCreateDTO;
import com.asset.system.code.dto.CodeRuleQueryDTO;
import com.asset.system.code.entity.SysCodeRule;
import com.asset.system.code.mapper.SysCodeRuleMapper;
import com.asset.system.code.service.SysCodeRuleService;
import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 业务编码规则 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysCodeRuleServiceImpl extends ServiceImpl<SysCodeRuleMapper, SysCodeRule>
        implements SysCodeRuleService {

    @Override
    public IPage<SysCodeRule> pageQuery(CodeRuleQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysCodeRule>()
                        .like(StringUtils.hasText(query.getRuleName()), SysCodeRule::getRuleName, query.getRuleName())
                        .like(StringUtils.hasText(query.getRuleKey()), SysCodeRule::getRuleKey, query.getRuleKey())
                        .eq(query.getStatus() != null, SysCodeRule::getStatus, query.getStatus())
                        .orderByAsc(SysCodeRule::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(CodeRuleCreateDTO dto) {
        long count = baseMapper.selectCount(new LambdaQueryWrapper<SysCodeRule>()
                .eq(SysCodeRule::getRuleKey, dto.getRuleKey()));
        if (count > 0) throw new SysBizException(SysErrorCode.CODE_RULE_KEY_EXISTS);

        SysCodeRule rule = new SysCodeRule();
        rule.setRuleKey(dto.getRuleKey());
        rule.setRuleName(dto.getRuleName());
        rule.setPrefix(dto.getPrefix() != null ? dto.getPrefix() : "");
        rule.setDateFormat(dto.getDateFormat() != null ? dto.getDateFormat() : "yyyyMM");
        rule.setSep(dto.getSep() != null ? dto.getSep() : "-");
        rule.setSeqLength(dto.getSeqLength());
        rule.setResetType(dto.getResetType());
        rule.setCurrentSeq(0L);
        rule.setCurrentPeriod("");
        rule.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        rule.setRemark(dto.getRemark());
        baseMapper.insert(rule);
        log.info("[编码规则] 新增规则 key={} name={}", rule.getRuleKey(), rule.getRuleName());
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(CodeRuleCreateDTO dto) {
        SysCodeRule exist = baseMapper.selectById(dto.getId());
        if (exist == null) throw new SysBizException(SysErrorCode.CODE_RULE_NOT_FOUND);

        update(new LambdaUpdateWrapper<SysCodeRule>()
                .eq(SysCodeRule::getId, dto.getId())
                .set(StringUtils.hasText(dto.getRuleName()), SysCodeRule::getRuleName, dto.getRuleName())
                .set(dto.getPrefix() != null, SysCodeRule::getPrefix, dto.getPrefix())
                .set(dto.getDateFormat() != null, SysCodeRule::getDateFormat, dto.getDateFormat())
                .set(dto.getSep() != null, SysCodeRule::getSep, dto.getSep())
                .set(dto.getSeqLength() != null, SysCodeRule::getSeqLength, dto.getSeqLength())
                .set(dto.getResetType() != null, SysCodeRule::getResetType, dto.getResetType())
                .set(dto.getStatus() != null, SysCodeRule::getStatus, dto.getStatus())
                .set(dto.getRemark() != null, SysCodeRule::getRemark, dto.getRemark()));
        log.info("[编码规则] 更新规则 id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.CODE_RULE_NOT_FOUND);
        removeById(id);
        log.info("[编码规则] 删除规则 id={}", id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.CODE_RULE_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysCodeRule>().eq(SysCodeRule::getId, id).set(SysCodeRule::getStatus, status));
    }

    /**
     * 生成业务编码（事务 + SELECT FOR UPDATE，保证序号唯一）
     * 格式：[prefix][sep][dateStr][sep][seqPadded]
     * 分隔符只在两段都存在时插入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateCode(String ruleKey) {
        SysCodeRule rule = baseMapper.selectByKeyForUpdate(ruleKey);
        if (rule == null) throw new SysBizException(SysErrorCode.CODE_RULE_NOT_FOUND);
        if (rule.getStatus() == 0) throw new SysBizException(SysErrorCode.CODE_RULE_DISABLED);

        // 计算当前周期字符串
        String currentPeriod = "";
        String dateStr = "";
        if (StringUtils.hasText(rule.getDateFormat())) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(rule.getDateFormat());
            dateStr = LocalDateTime.now().format(fmt);
            currentPeriod = dateStr;
        }

        // 判断是否需要重置序列号
        long nextSeq;
        if (needReset(rule, currentPeriod)) {
            nextSeq = 1L;
        } else {
            nextSeq = rule.getCurrentSeq() + 1;
        }

        // 更新序列号和周期
        baseMapper.updateSeq(rule.getId(), nextSeq, currentPeriod);

        // 拼接编码
        String sep = StringUtils.hasText(rule.getSep()) ? rule.getSep() : "-";
        String seqPadded = String.format("%0" + rule.getSeqLength() + "d", nextSeq);
        StringBuilder code = new StringBuilder();
        if (StringUtils.hasText(rule.getPrefix())) {
            code.append(rule.getPrefix());
        }
        if (StringUtils.hasText(dateStr)) {
            if (!code.isEmpty()) code.append(sep);
            code.append(dateStr);
        }
        if (!code.isEmpty()) code.append(sep);
        code.append(seqPadded);

        log.debug("[编码规则] 生成编码 ruleKey={} code={}", ruleKey, code);
        return code.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetSeq(Long id) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.CODE_RULE_NOT_FOUND);
        baseMapper.updateSeq(id, 0L, "");
        log.info("[编码规则] 手动重置序列号 id={}", id);
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private boolean needReset(SysCodeRule rule, String currentPeriod) {
        if (rule.getResetType() == 0) return false;
        // 有周期且周期发生变化 → 重置
        return StringUtils.hasText(currentPeriod)
                && !currentPeriod.equals(rule.getCurrentPeriod());
    }
}
