package com.asset.file.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 映射本地上传目录为静态资源路径
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileProperties props;

    /**
     * 将本地上传目录映射到 /file/** 访问路径
     * 例如：文件存储于 /opt/asset-uploads/2026/02/xxx.jpg
     *       可通过 /file/2026/02/xxx.jpg 访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/file/**")
                .addResourceLocations("file:" + props.getUploadDir() + "/");
    }
}
