package com.asset.base;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.asset")
@EnableDiscoveryClient
@MapperScan("com.asset.base.mapper")
public class AssetBaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetBaseApplication.class, args);
    }
}
