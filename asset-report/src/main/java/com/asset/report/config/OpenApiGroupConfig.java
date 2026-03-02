package com.asset.report.config;

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
    public OpenAPI reportOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("报表管理服务 API")
                .description("报表ETL + BI看板，端口8005")
                .version("v1.0.0"));
    }

    @Bean
    public GroupedOpenApi assetReportApi() {
        return GroupedOpenApi.builder()
                .group("01-资产类报表")
                .pathsToMatch("/rpt/asset/**")
                .build();
    }

    @Bean
    public GroupedOpenApi investmentReportApi() {
        return GroupedOpenApi.builder()
                .group("02-招商类报表")
                .pathsToMatch("/rpt/inv/**")
                .build();
    }

    @Bean
    public GroupedOpenApi operationReportApi() {
        return GroupedOpenApi.builder()
                .group("03-营运类报表")
                .pathsToMatch("/rpt/opr/**")
                .build();
    }

    @Bean
    public GroupedOpenApi financeReportApi() {
        return GroupedOpenApi.builder()
                .group("04-财务类报表")
                .pathsToMatch("/rpt/fin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi commonReportApi() {
        return GroupedOpenApi.builder()
                .group("05-通用能力")
                .pathsToMatch("/rpt/common/**")
                .build();
    }
}
