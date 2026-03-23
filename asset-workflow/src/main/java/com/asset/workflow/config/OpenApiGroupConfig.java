package com.asset.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc API 文档分组配置
 */
@Configuration
public class OpenApiGroupConfig {

    @Bean
    public OpenAPI workflowOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("工作流服务 API")
                .description("统一审批流程管理，端口8010")
                .version("v1.0.0"));
    }

    @Bean
    public GroupedOpenApi approvalApi() {
        return GroupedOpenApi.builder()
                .group("01-审批操作")
                .pathsToMatch("/wf/approvals/**")
                .build();
    }

    @Bean
    public GroupedOpenApi taskApi() {
        return GroupedOpenApi.builder()
                .group("02-待办任务")
                .pathsToMatch("/wf/tasks/**")
                .build();
    }

    @Bean
    public GroupedOpenApi processApi() {
        return GroupedOpenApi.builder()
                .group("03-流程监控")
                .pathsToMatch("/wf/processes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi definitionApi() {
        return GroupedOpenApi.builder()
                .group("04-流程定义")
                .pathsToMatch("/wf/definitions/**")
                .build();
    }
}
