package com.asset.finance;

import com.asset.api.workflow.ApprovalService;
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
                // 排除 Security / Redisson 自动配置
                "spring.autoconfigure.exclude=" +
                        "com.asset.common.security.config.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration," +
                        "org.redisson.spring.starter.RedissonAutoConfigurationV2"
        }
)
@ActiveProfiles("test")
@Transactional  // 每个测试自动回滚
public abstract class FinanceTestBase {

    @MockBean
    protected ApprovalService approvalService;

    /**
     * 审批服务默认行为：提交审批返回固定 approvalId
     */
    @BeforeEach
    void setupMocks() {
        when(approvalService.submit(any()))
                .thenAnswer(inv -> "MOCK-APPROVAL-" + System.currentTimeMillis());
    }
}
