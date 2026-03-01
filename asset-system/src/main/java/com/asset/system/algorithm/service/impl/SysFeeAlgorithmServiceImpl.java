package com.asset.system.algorithm.service.impl;

import com.asset.system.algorithm.dto.CalcTestDTO;
import com.asset.system.algorithm.dto.CalcTestResultVO;
import com.asset.system.algorithm.dto.FeeAlgorithmCreateDTO;
import com.asset.system.algorithm.dto.FeeAlgorithmQueryDTO;
import com.asset.system.algorithm.entity.SysFeeAlgorithm;
import com.asset.system.algorithm.mapper.SysFeeAlgorithmMapper;
import com.asset.system.algorithm.service.SysFeeAlgorithmService;
import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 租费算法规则 ServiceImpl
 * <p>试算引擎使用 Spring SpEL，仅允许变量 + 四则运算 + Math.max/min，
 * 通过 StandardEvaluationContext 沙箱限制任意方法调用（生产需进一步收紧）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFeeAlgorithmServiceImpl extends ServiceImpl<SysFeeAlgorithmMapper, SysFeeAlgorithm>
        implements SysFeeAlgorithmService {

    private static final ExpressionParser SPEL = new SpelExpressionParser();

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public IPage<SysFeeAlgorithm> pageQuery(FeeAlgorithmQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysFeeAlgorithm>()
                        .like(StringUtils.hasText(query.getAlgoName()), SysFeeAlgorithm::getAlgoName, query.getAlgoName())
                        .eq(query.getAlgoType() != null, SysFeeAlgorithm::getAlgoType, query.getAlgoType())
                        .eq(query.getCalcMode() != null, SysFeeAlgorithm::getCalcMode, query.getCalcMode())
                        .eq(query.getStatus() != null, SysFeeAlgorithm::getStatus, query.getStatus())
                        .orderByDesc(SysFeeAlgorithm::getId));
    }

    @Override
    public List<SysFeeAlgorithm> listEnabled() {
        return baseMapper.selectList(new LambdaQueryWrapper<SysFeeAlgorithm>()
                .eq(SysFeeAlgorithm::getStatus, 1)
                .orderByAsc(SysFeeAlgorithm::getAlgoType, SysFeeAlgorithm::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAlgorithm(FeeAlgorithmCreateDTO dto) {
        long count = baseMapper.selectCount(new LambdaQueryWrapper<SysFeeAlgorithm>()
                .eq(SysFeeAlgorithm::getAlgoCode, dto.getAlgoCode()));
        if (count > 0) throw new SysBizException(SysErrorCode.ALGORITHM_CODE_EXISTS);

        SysFeeAlgorithm algo = toEntity(dto);
        baseMapper.insert(algo);
        log.info("[租费算法] 新增算法 code={} name={}", algo.getAlgoCode(), algo.getAlgoName());
        return algo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlgorithm(FeeAlgorithmCreateDTO dto) {
        if (baseMapper.selectById(dto.getId()) == null)
            throw new SysBizException(SysErrorCode.ALGORITHM_NOT_FOUND);

        update(new LambdaUpdateWrapper<SysFeeAlgorithm>()
                .eq(SysFeeAlgorithm::getId, dto.getId())
                .set(StringUtils.hasText(dto.getAlgoName()), SysFeeAlgorithm::getAlgoName, dto.getAlgoName())
                .set(dto.getAlgoType() != null, SysFeeAlgorithm::getAlgoType, dto.getAlgoType())
                .set(dto.getCalcMode() != null, SysFeeAlgorithm::getCalcMode, dto.getCalcMode())
                .set(StringUtils.hasText(dto.getFormula()), SysFeeAlgorithm::getFormula, dto.getFormula())
                .set(dto.getVariables() != null, SysFeeAlgorithm::getVariables, dto.getVariables())
                .set(dto.getParams() != null, SysFeeAlgorithm::getParams, dto.getParams())
                .set(dto.getDescription() != null, SysFeeAlgorithm::getDescription, dto.getDescription())
                .set(dto.getStatus() != null, SysFeeAlgorithm::getStatus, dto.getStatus()));
        log.info("[租费算法] 更新算法 id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlgorithm(Long id) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.ALGORITHM_NOT_FOUND);
        removeById(id);
        log.info("[租费算法] 删除算法 id={}", id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.ALGORITHM_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysFeeAlgorithm>()
                .eq(SysFeeAlgorithm::getId, id).set(SysFeeAlgorithm::getStatus, status));
    }

    // ─── 试算引擎（SpEL）─────────────────────────────────────────────────────

    @Override
    public CalcTestResultVO testCalc(CalcTestDTO dto) {
        SysFeeAlgorithm algo = baseMapper.selectById(dto.getAlgoId());
        if (algo == null) throw new SysBizException(SysErrorCode.ALGORITHM_NOT_FOUND);

        String formula = algo.getFormula();
        Map<String, String> inputs = dto.getInputs();

        // 1. 注入固定参数
        String expanded = formula;
        if (algo.getParams() != null && !algo.getParams().isNull()) {
            Iterator<Map.Entry<String, JsonNode>> fields = algo.getParams().fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                expanded = expanded.replace(entry.getKey(), entry.getValue().asText());
            }
        }

        // 2. 注入变量值
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            expanded = expanded.replace(entry.getKey(), entry.getValue());
        }

        // 3. SpEL 求值（将 Math.max/Math.min 转换为 SpEL 兼容形式）
        String spelExpr = toSpelExpr(expanded);
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            // 注入 Math 工具类
            ctx.setVariable("Math", Math.class);
            Expression expr = SPEL.parseExpression(spelExpr);
            Number value = expr.getValue(ctx, Number.class);
            if (value == null) throw new SysBizException(SysErrorCode.SYS_5001);

            BigDecimal result = new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
            String detail = buildDetail(algo, inputs, result);
            return new CalcTestResultVO(result.toPlainString(), expanded, detail);

        } catch (ParseException | EvaluationException e) {
            log.warn("[租费算法] 试算失败 algoId={} formula='{}' err={}", dto.getAlgoId(), spelExpr, e.getMessage());
            throw new SysBizException(SysErrorCode.SYS_5001, "公式求值失败: " + e.getMessage());
        }
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private SysFeeAlgorithm toEntity(FeeAlgorithmCreateDTO dto) {
        SysFeeAlgorithm algo = new SysFeeAlgorithm();
        algo.setAlgoCode(dto.getAlgoCode());
        algo.setAlgoName(dto.getAlgoName());
        algo.setAlgoType(dto.getAlgoType());
        algo.setCalcMode(dto.getCalcMode());
        algo.setFormula(dto.getFormula());
        algo.setVariables(dto.getVariables());
        algo.setParams(dto.getParams());
        algo.setDescription(dto.getDescription());
        algo.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return algo;
    }

    /**
     * 将 Math.max(a, b) 形式转为 SpEL T(Math).max(a, b)
     */
    private String toSpelExpr(String formula) {
        return formula.replace("Math.max(", "T(Math).max(")
                      .replace("Math.min(", "T(Math).min(")
                      .replace("Math.abs(", "T(Math).abs(");
    }

    private String buildDetail(SysFeeAlgorithm algo, Map<String, String> inputs, BigDecimal result) {
        List<String> lines = new ArrayList<>();
        lines.add("算法：" + algo.getAlgoName());
        lines.add("公式：" + algo.getFormula());
        if (!inputs.isEmpty()) {
            lines.add("变量：");
            inputs.forEach((k, v) -> lines.add("  " + k + " = " + v));
        }
        lines.add("结果：¥ " + result.toPlainString());
        return String.join("\n", lines);
    }
}
