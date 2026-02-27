package com.asset.finance.config;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiGroupConfig {
    @Bean public GroupedOpenApi receivableApi() { return GroupedOpenApi.builder().group("01-应收管理").addOpenApiCustomizer(o -> o.info(new Info().title("应收管理").version("1.0"))).pathsToMatch("/fin/receivables/**").build(); }
    @Bean public GroupedOpenApi receiptApi() { return GroupedOpenApi.builder().group("02-收款管理").addOpenApiCustomizer(o -> o.info(new Info().title("收款管理").version("1.0"))).pathsToMatch("/fin/receipts/**", "/fin/write-offs/**").build(); }
    @Bean public GroupedOpenApi voucherApi() { return GroupedOpenApi.builder().group("03-凭证管理").addOpenApiCustomizer(o -> o.info(new Info().title("凭证管理").version("1.0"))).pathsToMatch("/fin/vouchers/**").build(); }
    @Bean public GroupedOpenApi depositApi() { return GroupedOpenApi.builder().group("04-保证金管理").addOpenApiCustomizer(o -> o.info(new Info().title("保证金管理").version("1.0"))).pathsToMatch("/fin/deposits/**").build(); }
    @Bean public GroupedOpenApi prepaymentApi() { return GroupedOpenApi.builder().group("05-预收款管理").addOpenApiCustomizer(o -> o.info(new Info().title("预收款管理").version("1.0"))).pathsToMatch("/fin/prepayments/**").build(); }
    @Bean public GroupedOpenApi reductionApi() { return GroupedOpenApi.builder().group("06-减免调整").addOpenApiCustomizer(o -> o.info(new Info().title("减免调整").version("1.0"))).pathsToMatch("/fin/reductions/**").build(); }
}
