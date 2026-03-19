package com.asset.system.algorithm;

import com.asset.system.algorithm.dto.CalcTestDTO;
import com.asset.system.algorithm.dto.CalcTestResultVO;
import com.asset.system.algorithm.dto.FeeAlgorithmCreateDTO;
import com.asset.system.algorithm.entity.SysFeeAlgorithm;
import com.asset.system.algorithm.mapper.SysFeeAlgorithmMapper;
import com.asset.system.algorithm.service.impl.SysFeeAlgorithmServiceImpl;
import com.asset.system.common.exception.SysBizException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.10 租费算法 — Service 单元测试
 * ALGO-U-01 ~ ALGO-U-08
 *
 * 实际实现：通用 SpEL 公式引擎（无阶梯 Step Mapper），测试覆盖试算引擎和 CRUD。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.10 租费算法 Service 单元测试")
class SysFeeAlgorithmServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_algo_ns");
        TableInfoHelper.initTableInfo(assistant, SysFeeAlgorithm.class);
    }

    @Mock SysFeeAlgorithmMapper algoMapper;

    @InjectMocks SysFeeAlgorithmServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", algoMapper);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysFeeAlgorithm algo(Long id, String code, String name, String formula) {
        SysFeeAlgorithm a = new SysFeeAlgorithm();
        a.setId(id);
        a.setAlgoCode(code);
        a.setAlgoName(name);
        a.setAlgoType(1);
        a.setCalcMode(4);
        a.setFormula(formula);
        a.setStatus(1);
        return a;
    }

    // ─── ALGO-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("ALGO-U-01 试算-固定租金：unit_price*area*months = 5000")
    void testCalc_fixed_rent_success() {
        when(algoMapper.selectById(91001L))
                .thenReturn(algo(91001L, "ALG_FIXED", "固定租金", "unit_price * area * months"));

        CalcTestDTO dto = new CalcTestDTO();
        dto.setAlgoId(91001L);
        dto.setInputs(Map.of("unit_price", "50", "area", "100", "months", "1"));

        CalcTestResultVO result = service.testCalc(dto);

        assertThat(result.getResult()).isEqualTo("5000.00");
    }

    // ─── ALGO-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("ALGO-U-02 试算-浮动提成：revenue*rate/100 = 16000")
    void testCalc_floating_commission_success() {
        when(algoMapper.selectById(91002L))
                .thenReturn(algo(91002L, "ALG_FLOATING", "浮动租金", "revenue * rate / 100"));

        CalcTestDTO dto = new CalcTestDTO();
        dto.setAlgoId(91002L);
        dto.setInputs(Map.of("revenue", "200000", "rate", "8"));

        CalcTestResultVO result = service.testCalc(dto);

        assertThat(result.getResult()).isEqualTo("16000.00");
    }

    // ─── ALGO-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("ALGO-U-03 试算-取高算法：Math.max(fixed, floating) = 8000")
    void testCalc_mathMax_returnsHigher() {
        when(algoMapper.selectById(91003L))
                .thenReturn(algo(91003L, "ALG_HIGHER", "取高算法", "Math.max(fixed_rent, floating_rent)"));

        CalcTestDTO dto = new CalcTestDTO();
        dto.setAlgoId(91003L);
        dto.setInputs(Map.of("fixed_rent", "5000", "floating_rent", "8000"));

        CalcTestResultVO result = service.testCalc(dto);

        assertThat(result.getResult()).isEqualTo("8000.00");
    }

    // ─── ALGO-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("ALGO-U-04 试算-保底算法：Math.max(revenue*rate/100, min_amount) = 5000")
    void testCalc_withMinGuarantee_returnsMax() {
        when(algoMapper.selectById(91004L))
                .thenReturn(algo(91004L, "ALG_COMMISSION", "保底提成", "Math.max(revenue * rate / 100, min_amount)"));

        CalcTestDTO dto = new CalcTestDTO();
        dto.setAlgoId(91004L);
        // revenue*rate/100 = 100000*3/100 = 3000, min_amount=5000 → max=5000
        dto.setInputs(Map.of("revenue", "100000", "rate", "3", "min_amount", "5000"));

        CalcTestResultVO result = service.testCalc(dto);

        assertThat(result.getResult()).isEqualTo("5000.00");
    }

    // ─── ALGO-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("ALGO-U-05 新增算法：insert 被调用，返回新ID")
    void createAlgorithm_success() {
        when(algoMapper.selectCount(any())).thenReturn(0L);
        when(algoMapper.insert(any(SysFeeAlgorithm.class))).thenReturn(1);

        FeeAlgorithmCreateDTO dto = new FeeAlgorithmCreateDTO();
        dto.setAlgoCode("NEW_ALG");
        dto.setAlgoName("新算法");
        dto.setAlgoType(1);
        dto.setCalcMode(4);
        dto.setFormula("price * qty");

        service.createAlgorithm(dto);

        verify(algoMapper).insert(any(SysFeeAlgorithm.class));
    }

    // ─── ALGO-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("ALGO-U-06 新增-编码重复：抛出编码已存在异常")
    void createAlgorithm_duplicateCode_throws() {
        when(algoMapper.selectCount(any())).thenReturn(1L);

        FeeAlgorithmCreateDTO dto = new FeeAlgorithmCreateDTO();
        dto.setAlgoCode("ALG_RENT_FIXED");
        dto.setAlgoName("固定租金副本");
        dto.setAlgoType(1);
        dto.setCalcMode(4);
        dto.setFormula("unit_price * area");

        assertThatThrownBy(() -> service.createAlgorithm(dto))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("已存在");
    }

    // ─── ALGO-U-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("ALGO-U-07 删除算法：removeById 被调用")
    void deleteAlgorithm_success() {
        when(algoMapper.selectById(91001L))
                .thenReturn(algo(91001L, "ALG_FIXED", "固定租金", "unit_price * area * months"));
        when(algoMapper.deleteById(91001L)).thenReturn(1);

        service.deleteAlgorithm(91001L);

        verify(algoMapper).deleteById(91001L);
    }

    // ─── ALGO-U-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("ALGO-U-08 试算-算法不存在：抛出异常")
    void testCalc_algoNotFound_throws() {
        when(algoMapper.selectById(99999L)).thenReturn(null);

        CalcTestDTO dto = new CalcTestDTO();
        dto.setAlgoId(99999L);
        dto.setInputs(Map.of("a", "1"));

        assertThatThrownBy(() -> service.testCalc(dto))
                .isInstanceOf(SysBizException.class);
    }
}
