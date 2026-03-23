package com.asset.workflow.config;

import lombok.extern.slf4j.Slf4j;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable 引擎配置
 */
@Slf4j
@Configuration
public class FlowableConfig {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> engineConfigurer() {
        return configuration -> {
            configuration.setActivityFontName("宋体");
            configuration.setLabelFontName("宋体");
            configuration.setAnnotationFontName("宋体");
            log.info("[Flowable] 引擎配置完成，字体设置为宋体");
        };
    }
}
