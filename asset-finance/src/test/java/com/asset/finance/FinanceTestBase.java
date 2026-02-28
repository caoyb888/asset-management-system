package com.asset.finance;

import com.asset.finance.common.adapter.OaApprovalAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 财务模块单元测试基类
 *
 * <p>使用 H2 内存库（MySQL 兼容模式），禁用 Nacos/Redis/Security，
 * Mock OA 审批适配器（始终返回测试用 approvalId）。
 * 每个测试方法在独立事务中运行并回滚，保证数据隔离。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.discovery.register-enabled=false",
                // 排除 Security 自动配置（NONE 环境下无 HttpSecurity bean）
                "spring.autoconfigure.exclude=" +
                        "com.asset.common.security.config.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"
        }
)
@ActiveProfiles("test")
@Transactional  // 每个测试自动回滚
public abstract class FinanceTestBase {

    @MockBean
    protected OaApprovalAdapter oaApprovalAdapter;

    /**
     * OA 适配器默认行为：提交审批返回固定 approvalId，查询状态返回 PENDING
     */
    @BeforeEach
    void setupMocks() {
        when(oaApprovalAdapter.submitApproval(anyString(), anyLong(), anyString()))
                .thenAnswer(inv -> "MOCK-APPROVAL-" + inv.getArgument(1));
    }
}
