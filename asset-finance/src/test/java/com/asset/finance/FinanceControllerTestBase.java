package com.asset.finance;

import com.asset.finance.common.adapter.OaApprovalAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 财务模块 Controller 集成测试基类
 *
 * <p>使用 MockMvc + H2 内存库（MySQL 兼容模式），禁用 Nacos/Redis/Security，
 * Mock OA 审批适配器。每个测试方法在独立事务中运行并回滚，保证数据隔离。
 *
 * <p>与 {@link FinanceTestBase} 的区别：
 * <ul>
 *   <li>webEnvironment = MOCK（启动完整 Web 层，支持 MockMvc）</li>
 *   <li>addFilters = false（绕过 JWT 安全过滤器）</li>
 *   <li>注入 MockMvc 和 ObjectMapper，方便子类发送 HTTP 请求</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = FinanceApplication.class,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.discovery.register-enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                // 排除 Security / Redisson 自动配置
                "spring.autoconfigure.exclude=" +
                        "com.asset.common.security.config.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration," +
                        "org.redisson.spring.starter.RedissonAutoConfigurationV2"
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional  // 每个测试自动回滚
public abstract class FinanceControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected OaApprovalAdapter oaApprovalAdapter;

    /**
     * OA 适配器默认行为：提交审批返回固定 approvalId
     */
    @BeforeEach
    void setupOaMock() {
        when(oaApprovalAdapter.submitApproval(anyString(), anyLong(), anyString()))
                .thenAnswer(inv -> "MOCK-APPROVAL-" + inv.getArgument(1));
    }
}
