package com.asset.finance.common;

import com.asset.finance.common.util.FinCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * 单号生成器单元测试
 *
 * <p>纯 JUnit 5，直接 new 实例，不依赖 Spring 上下文。
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>COM-01：收款单号格式 RC-yyyyMMdd-000001</li>
 *   <li>COM-02：核销单号格式 WO-yyyyMMdd-000001</li>
 *   <li>COM-03：凭证号格式 VC-yyyyMMdd-000001</li>
 *   <li>COM-04：100次连续生成无重复</li>
 * </ol>
 */
@DisplayName("单号生成器测试")
class FinCodeGeneratorTest {

    private FinCodeGenerator generator;

    /** 当日日期字符串，用于正则匹配 */
    private String todayStr;

    @BeforeEach
    void setUp() {
        generator = new FinCodeGenerator();
        todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    // ─── COM-01：收款单号格式正确 ──────────────────────────────────────────────

    @Test
    @DisplayName("COM-01：收款单号格式 RC-yyyyMMdd-xxxxxx")
    void receiptCode_shouldMatchFormat() {
        String code = generator.receiptCode();

        assertThat(code)
                .as("收款单号应匹配 RC-日期-6位序号 格式")
                .matches("RC-" + todayStr + "-\\d{6}");
    }

    // ─── COM-02：核销单号格式正确 ──────────────────────────────────────────────

    @Test
    @DisplayName("COM-02：核销单号格式 WO-yyyyMMdd-xxxxxx")
    void writeOffCode_shouldMatchFormat() {
        String code = generator.writeOffCode();

        assertThat(code)
                .as("核销单号应匹配 WO-日期-6位序号 格式")
                .matches("WO-" + todayStr + "-\\d{6}");
    }

    // ─── COM-03：凭证号格式正确 ────────────────────────────────────────────────

    @Test
    @DisplayName("COM-03：凭证号格式 VC-yyyyMMdd-xxxxxx")
    void voucherCode_shouldMatchFormat() {
        String code = generator.voucherCode();

        assertThat(code)
                .as("凭证号应匹配 VC-日期-6位序号 格式")
                .matches("VC-" + todayStr + "-\\d{6}");
    }

    // ─── COM-04：100次连续生成无重复 ───────────────────────────────────────────

    @Test
    @DisplayName("COM-04：100次连续生成收款单号无重复")
    void generate100Codes_shouldBeUnique() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codes.add(generator.receiptCode());
        }

        assertThat(codes)
                .as("100次生成应产生100个不同的编号")
                .hasSize(100);
    }
}
