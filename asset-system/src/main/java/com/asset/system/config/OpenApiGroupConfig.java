package com.asset.system.config;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/** 系统管理模块 API 分组配置 */
@Configuration
public class OpenApiGroupConfig {
    @Bean public GroupedOpenApi authApi() { return GroupedOpenApi.builder().group("00-认证管理").addOpenApiCustomizer(o -> o.info(new Info().title("认证管理").version("1.0"))).pathsToMatch("/auth/**").build(); }
    @Bean public GroupedOpenApi userApi() { return GroupedOpenApi.builder().group("01-用户管理").addOpenApiCustomizer(o -> o.info(new Info().title("用户管理").version("1.0"))).pathsToMatch("/sys/users/**").build(); }
    @Bean public GroupedOpenApi deptApi() { return GroupedOpenApi.builder().group("02-机构管理").addOpenApiCustomizer(o -> o.info(new Info().title("机构管理").version("1.0"))).pathsToMatch("/sys/depts/**").build(); }
    @Bean public GroupedOpenApi postApi() { return GroupedOpenApi.builder().group("03-岗位管理").addOpenApiCustomizer(o -> o.info(new Info().title("岗位管理").version("1.0"))).pathsToMatch("/sys/posts/**").build(); }
    @Bean public GroupedOpenApi roleApi() { return GroupedOpenApi.builder().group("04-角色管理").addOpenApiCustomizer(o -> o.info(new Info().title("角色管理").version("1.0"))).pathsToMatch("/sys/roles/**").build(); }
    @Bean public GroupedOpenApi menuApi() { return GroupedOpenApi.builder().group("05-菜单管理").addOpenApiCustomizer(o -> o.info(new Info().title("菜单管理").version("1.0"))).pathsToMatch("/sys/menus/**").build(); }
    @Bean public GroupedOpenApi dictApi() { return GroupedOpenApi.builder().group("06-业务字典").addOpenApiCustomizer(o -> o.info(new Info().title("业务字典").version("1.0"))).pathsToMatch("/sys/dict/**").build(); }
    @Bean public GroupedOpenApi logApi()  { return GroupedOpenApi.builder().group("07-操作日志").addOpenApiCustomizer(o -> o.info(new Info().title("操作日志").version("1.0"))).pathsToMatch("/sys/logs/**").build(); }
    @Bean public GroupedOpenApi codeApi() { return GroupedOpenApi.builder().group("08-编码规则管理").addOpenApiCustomizer(o -> o.info(new Info().title("编码规则管理").version("1.0"))).pathsToMatch("/sys/code-rules/**").build(); }
    @Bean public GroupedOpenApi algoApi()   { return GroupedOpenApi.builder().group("09-租费算法管理").addOpenApiCustomizer(o -> o.info(new Info().title("租费算法管理").version("1.0"))).pathsToMatch("/sys/fee-algorithms/**").build(); }
    @Bean public GroupedOpenApi configApi() { return GroupedOpenApi.builder().group("10-系统参数配置").addOpenApiCustomizer(o -> o.info(new Info().title("系统参数配置").version("1.0"))).pathsToMatch("/sys/configs/**").build(); }
}
