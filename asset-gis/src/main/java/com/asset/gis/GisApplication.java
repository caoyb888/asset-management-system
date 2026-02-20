package com.asset.gis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.asset")
@EnableDiscoveryClient
public class GisApplication {
    public static void main(String[] args) {
        SpringApplication.run(GisApplication.class, args);
    }
}
