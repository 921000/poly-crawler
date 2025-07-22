package com.poly.crawler.config;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


/**
 * ThreadPoolConfig 线程池配置
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Configuration
public class CrawlerThreadPoolConfig {

    @Value("${crawler.thread.pool.corePoolSize:60}")
    private int corePoolSize;

    @Value("${crawler.thread.pool.maxPoolSize:120}")
    private int maxPoolSize;

    @Value("${crawler.thread.pool.queueCapacity:1000}")
    private int queueCapacity;

    @Bean(name = "crawlerTaskExecutor")
    public ThreadPoolTaskExecutor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("crawlerTaskExecutor-");

        // 设置默认的拒绝策略 线程池和任务队列都已满，新的任务尝试提交时 抛异常
        RejectedExecutionHandler rejectedExecutionHandler = new AbortPolicy();
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);

        executor.initialize();
        return executor;
    }

}
