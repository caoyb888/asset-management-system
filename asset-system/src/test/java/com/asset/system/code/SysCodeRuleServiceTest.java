package com.asset.system.code;

import com.asset.system.code.dto.CodeRuleCreateDTO;
import com.asset.system.code.entity.SysCodeRule;
import com.asset.system.code.mapper.SysCodeRuleMapper;
import com.asset.system.code.service.impl.SysCodeRuleServiceImpl;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.9 编码规则 — Service 单元测试
 * CODE-U-01 ~ CODE-U-07
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.9 编码规则 Service 单元测试")
class SysCodeRuleServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_code_ns");
        TableInfoHelper.initTableInfo(assistant, SysCodeRule.class);
    }

    @Mock SysCodeRuleMapper codeRuleMapper;

    @InjectMocks SysCodeRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", codeRuleMapper);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysCodeRule rule(Long id, String key, String prefix, String dateFormat,
                              String sep, int seqLength, int resetType,
                              long currentSeq, String currentPeriod) {
        SysCodeRule r = new SysCodeRule();
        r.setId(id);
        r.setRuleKey(key);
        r.setPrefix(prefix);
        r.setDateFormat(dateFormat);
        r.setSep(sep);
        r.setSeqLength(seqLength);
        r.setResetType(resetType);
        r.setCurrentSeq(currentSeq);
        r.setCurrentPeriod(currentPeriod);
        r.setStatus(1);
        return r;
    }

    private String todayStr(String fmt) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));
    }

    // ─── CODE-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("CODE-U-01 生成编码-日期+序号：格式 HT-{yyyyMMdd}-0001")
    void generate_withDateAndSeq_success() {
        when(codeRuleMapper.selectByKeyForUpdate("TEST_CONTRACT"))
                .thenReturn(rule(91001L, "TEST_CONTRACT", "HT", "yyyyMMdd", "-", 4, 2, 0L, ""));
        when(codeRuleMapper.updateSeq(anyLong(), anyLong(), anyString())).thenReturn(1);

        String code = service.generateCode("TEST_CONTRACT");

        assertThat(code).startsWith("HT-")
                        .endsWith("-0001")
                        .matches("HT-\\d{8}-0001");
        verify(codeRuleMapper).updateSeq(eq(91001L), eq(1L), eq(todayStr("yyyyMMdd")));
    }

    // ─── CODE-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("CODE-U-02 生成编码-序号递增：第2次序号=第1次+1")
    void generate_sequential_increment() {
        String today = todayStr("yyyyMMdd");
        // First call: seq=0, period=today (same) → seq becomes 1
        // Second call: seq=1, period=today (same) → seq becomes 2
        when(codeRuleMapper.selectByKeyForUpdate("TEST_CONTRACT"))
                .thenReturn(
                        rule(91001L, "TEST_CONTRACT", "HT", "yyyyMMdd", "-", 4, 2, 0L, today),
                        rule(91001L, "TEST_CONTRACT", "HT", "yyyyMMdd", "-", 4, 2, 1L, today));
        when(codeRuleMapper.updateSeq(anyLong(), anyLong(), anyString())).thenReturn(1);

        String code1 = service.generateCode("TEST_CONTRACT");
        String code2 = service.generateCode("TEST_CONTRACT");

        assertThat(code1).endsWith("-0001");
        assertThat(code2).endsWith("-0002");
    }

    // ─── CODE-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("CODE-U-03 生成编码-月重置：跨期后序号从1重新开始")
    void generate_monthReset_seqRestartsFromOne() {
        // currentPeriod is "20260201" (old), today is "20260319" (different) → needReset=true
        when(codeRuleMapper.selectByKeyForUpdate("TEST_CONTRACT"))
                .thenReturn(rule(91001L, "TEST_CONTRACT", "HT", "yyyyMMdd", "-", 4, 2, 42L, "20260201"));
        when(codeRuleMapper.updateSeq(anyLong(), anyLong(), anyString())).thenReturn(1);

        String code = service.generateCode("TEST_CONTRACT");

        // seq resets to 1
        assertThat(code).endsWith("-0001");
        verify(codeRuleMapper).updateSeq(eq(91001L), eq(1L), anyString());
    }

    // ─── CODE-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("CODE-U-04 生成编码-不重置：resetType=0 序号持续递增")
    void generate_noReset_continuesSeq() {
        // resetType=0 → no reset, even if period differs
        when(codeRuleMapper.selectByKeyForUpdate("TEST_RECEIPT"))
                .thenReturn(rule(91002L, "TEST_RECEIPT", "SK", "yyyyMMdd", "-", 4, 0, 99L, "20260201"));
        when(codeRuleMapper.updateSeq(anyLong(), anyLong(), anyString())).thenReturn(1);

        String code = service.generateCode("TEST_RECEIPT");

        assertThat(code).endsWith("-0100");
        verify(codeRuleMapper).updateSeq(eq(91002L), eq(100L), anyString());
    }

    // ─── CODE-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("CODE-U-05 规则不存在：抛出编码规则不存在异常")
    void generate_ruleNotFound_throws() {
        when(codeRuleMapper.selectByKeyForUpdate("NOT_EXIST")).thenReturn(null);

        assertThatThrownBy(() -> service.generateCode("NOT_EXIST"))
                .isInstanceOf(SysBizException.class);
    }

    // ─── CODE-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("CODE-U-06 手动重置序列号：updateSeq(id,0,'') 被调用")
    void resetSeq_success() {
        when(codeRuleMapper.selectById(91001L))
                .thenReturn(rule(91001L, "TEST_CONTRACT", "HT", "yyyyMMdd", "-", 4, 2, 42L, "20260319"));
        when(codeRuleMapper.updateSeq(91001L, 0L, "")).thenReturn(1);

        service.resetSeq(91001L);

        verify(codeRuleMapper).updateSeq(91001L, 0L, "");
    }

    // ─── CODE-U-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("CODE-U-07 新增-编码重复：抛出编码已存在异常")
    void createRule_duplicateCode_throws() {
        when(codeRuleMapper.selectCount(any())).thenReturn(1L);

        CodeRuleCreateDTO dto = new CodeRuleCreateDTO();
        dto.setRuleKey("TEST_CONTRACT");
        dto.setRuleName("合同编码副本");
        dto.setSeqLength(4);
        dto.setResetType(2);

        assertThatThrownBy(() -> service.createRule(dto))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("已存在");
    }
}
