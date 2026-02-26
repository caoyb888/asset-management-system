package com.asset.operation.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 营运管理模块 Knife4j / SpringDoc API 分组配置 */
@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi ledgerApi() {
        return GroupedOpenApi.builder()
                .group("01-合同台账")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("合同台账管理").version("1.0")))
                .pathsToMatch("/opr/ledgers/**")
                .build();
    }

    @Bean
    public GroupedOpenApi changeApi() {
        return GroupedOpenApi.builder()
                .group("02-合同变更")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("合同变更管理").version("1.0")))
                .pathsToMatch("/opr/contract-changes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi revenueApi() {
        return GroupedOpenApi.builder()
                .group("03-营收填报")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("营收填报与浮动租金").version("1.0")))
                .pathsToMatch("/opr/revenue-reports/**", "/opr/floating-rent/**")
                .build();
    }

    @Bean
    public GroupedOpenApi flowApi() {
        return GroupedOpenApi.builder()
                .group("04-客流填报")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("客流填报管理").version("1.0")))
                .pathsToMatch("/opr/passenger-flows/**")
                .build();
    }

    @Bean
    public GroupedOpenApi terminationApi() {
        return GroupedOpenApi.builder()
                .group("05-合同解约")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("合同解约管理").version("1.0")))
                .pathsToMatch("/opr/terminations/**")
                .build();
    }
}
