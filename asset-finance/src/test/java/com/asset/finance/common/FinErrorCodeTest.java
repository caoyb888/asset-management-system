package com.asset.finance.common;

import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 错误码与异常单元测试
 *
 * <p>纯 JUnit 5，直接 new 实例，不依赖 Spring 上下文。
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>COM-07：错误码枚举覆盖完整——每个值都有非空 code 和 message</li>
 *   <li>COM-08：FinBizException 携带正确错误码和描述</li>
 * </ol>
 */
@DisplayName("错误码与异常测试")
class FinErrorCodeTest {

    // ─── COM-07：错误码枚举覆盖完整 ──────────────────────────────────────────

    @Test
    @DisplayName("COM-07：所有 FinErrorCode 枚举值都有非空 code 和 message")
    void allErrorCodes_shouldHaveNonEmptyCodeAndMessage() {
        for (FinErrorCode errorCode : FinErrorCode.values()) {
            assertThat(errorCode.getCode())
                    .as("错误码 %s 的 code 应大于0", errorCode.name())
                    .isGreaterThan(0);

            assertThat(errorCode.getMessage())
                    .as("错误码 %s 的 message 不应为空", errorCode.name())
                    .isNotBlank();
        }
    }

    // ─── COM-08：FinBizException 携带正确错误码 ──────────────────────────────

    @Test
    @DisplayName("COM-08：FinBizException 携带正确的 code 和 message")
    void finBizException_shouldCarryCorrectCodeAndMessage() {
        // 基础构造函数
        FinBizException ex1 = new FinBizException(FinErrorCode.FIN_4001);
        assertThat(ex1.getCode())
                .as("异常 code 应等于 FIN_4001 的 code")
                .isEqualTo(FinErrorCode.FIN_4001.getCode());
        assertThat(ex1.getMessage())
                .as("异常 message 应包含错误描述")
                .contains("核销金额超过收款余额");

        // 带详情的构造函数
        FinBizException ex2 = new FinBizException(FinErrorCode.FIN_4002, "当前余额500，请求冲抵800");
        assertThat(ex2.getCode())
                .as("异常 code 应等于 FIN_4002 的 code")
                .isEqualTo(FinErrorCode.FIN_4002.getCode());
        assertThat(ex2.getMessage())
                .as("异常 message 应包含原始描述和附加详情")
                .contains("保证金余额不足")
                .contains("当前余额500，请求冲抵800");
    }
}
