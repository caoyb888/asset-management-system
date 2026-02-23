package com.asset.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "asset.file")
@Data
public class FileProperties {

    /** 文件本地存储根目录 */
    private String uploadDir = "/opt/asset-uploads";

    /** 文件访问URL前缀 */
    private String accessUrlPrefix = "/file";
}
