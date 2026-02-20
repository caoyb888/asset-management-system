package com.asset.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AssetGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetGatewayApplication.class, args);
    }
}
