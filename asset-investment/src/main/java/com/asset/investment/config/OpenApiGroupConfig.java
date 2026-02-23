package com.asset.investment.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 分组配置
 * 按招商模块5个子模块分组，方便按子模块查阅接口文档
 */
@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi configApi() {
        return GroupedOpenApi.builder()
                .group("01-配置管理")
                .displayName("配置管理（计租方案/收款项目）")
                .pathsToMatch("/inv/config/**")
                .build();
    }

    @Bean
    public GroupedOpenApi intentionApi() {
        return GroupedOpenApi.builder()
                .group("02-意向协议")
                .displayName("意向协议管理")
                .pathsToMatch("/inv/intentions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi contractApi() {
        return GroupedOpenApi.builder()
                .group("03-招商合同")
                .displayName("招商合同管理")
                .pathsToMatch("/inv/contracts/**")
                .build();
    }

    @Bean
    public GroupedOpenApi openingApi() {
        return GroupedOpenApi.builder()
                .group("04-开业审批")
                .displayName("开业审批管理")
                .pathsToMatch("/inv/opening-approvals/**")
                .build();
    }

    @Bean
    public GroupedOpenApi policyApi() {
        return GroupedOpenApi.builder()
                .group("05-租决与分解")
                .displayName("租决政策与租金分解")
                .pathsToMatch("/inv/rent-policies/**", "/inv/rent-decomps/**")
                .build();
    }
}
