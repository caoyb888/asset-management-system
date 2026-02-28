package com.asset.finance.common;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.common.annotation.OptimisticLockRetry;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.*;

/**
 * 乐观锁重试切面单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>超出最大重试次数 → 抛出 FIN_5002</li>
 *   <li>首次成功无需重试 → 正常返回</li>
 * </ol>
 */
@DisplayName("乐观锁重试切面测试")
@Import(OptimisticLockRetryAspectTest.TestBeans.class)
class OptimisticLockRetryAspectTest extends FinanceTestBase {

    @Autowired
    private AlwaysFailService alwaysFailService;

    @Autowired
    private SucceedOnThirdService succeedOnThirdService;

    // ─── 场景1：始终失败 → 超限后抛 FIN_5002 ─────────────────────────────────

    @Test
    @DisplayName("场景1：乐观锁持续冲突超过最大重试次数 → 抛出 FIN_5002")
    void alwaysFail_exceedsMaxRetries_shouldThrowFIN5002() {
        assertThatThrownBy(() -> alwaysFailService.doUpdate())
                .isInstanceOf(FinBizException.class)
                .extracting(e -> ((FinBizException) e).getCode())
                .isEqualTo(FinErrorCode.FIN_5002.getCode());
    }

    // ─── 场景2：第3次成功 → 正常返回结果 ────────────────────────────────────

    @Test
    @DisplayName("场景2：第3次重试成功 → 正常返回，无异常")
    void succeedOnThird_shouldReturnSuccess() {
        assertThatCode(() -> succeedOnThirdService.doUpdate())
                .doesNotThrowAnyException();
    }

    // ─── TestConfiguration：显式注册测试用 Service Bean ───────────────────────

    @TestConfiguration
    static class TestBeans {
        @Bean
        AlwaysFailService alwaysFailService() {
            return new AlwaysFailService();
        }

        @Bean
        SucceedOnThirdService succeedOnThirdService() {
            return new SucceedOnThirdService();
        }
    }

    // ─── 内嵌测试 Service ─────────────────────────────────────────────────────

    /**
     * 每次调用都抛乐观锁异常的测试 Service
     */
    static class AlwaysFailService {
        @OptimisticLockRetry(maxRetries = 3)
        public void doUpdate() {
            throw new OptimisticLockingFailureException("模拟乐观锁冲突：version mismatch");
        }
    }

    /**
     * 第3次调用才成功的测试 Service（前2次抛乐观锁异常）
     */
    static class SucceedOnThirdService {
        private int callCount = 0;

        @OptimisticLockRetry(maxRetries = 3)
        public void doUpdate() {
            callCount++;
            if (callCount < 3) {
                throw new OptimisticLockingFailureException("模拟冲突，第" + callCount + "次");
            }
            // 第3次成功
        }
    }
}
