package com.asset.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 报表导出配置
 * <p>
 * 提供专用异步线程池（rpt-export-*）和导出文件存储目录配置。
 * </p>
 */
@EnableAsync
@Configuration
public class ReportExportConfig {

    /**
     * 导出文件根目录，默认使用系统临时目录下的 rpt-export 子目录。
     * 生产环境建议改为 NFS/OSS 挂载路径。
     */
    @Value("${report.export.dir:${java.io.tmpdir}/rpt-export}")
    private String exportDir;

    @Bean("reportExportExecutor")
    public ThreadPoolTaskExecutor reportExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程：允许同时进行 3 个导出任务
        executor.setCorePoolSize(3);
        // 最大线程：高峰期允许最多 8 个
        executor.setMaxPoolSize(8);
        // 队列容量：最多等待 100 个任务
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("rpt-export-");
        // 拒绝策略：由调用方线程执行（保证不丢任务）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 确保导出目录存在，并返回规范路径。
     */
    public String getOrCreateExportDir() {
        File dir = new File(exportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    public String getExportDir() {
        return exportDir;
    }
}
