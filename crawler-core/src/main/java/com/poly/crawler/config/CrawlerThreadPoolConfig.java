package com.poly.crawler.config;

import com.poly.crawler.properties.CrawlerProperties;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import javax.annotation.Resource;
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

    @Resource
    private CrawlerProperties crawlerProperties;

    @Bean(name = "crawlerTaskExecutor")
    public ThreadPoolTaskExecutor crawlerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(crawlerProperties.getThread().getPool().getCorePoolSize());
        executor.setMaxPoolSize(crawlerProperties.getThread().getPool().getMaxPoolSize());
        executor.setQueueCapacity(crawlerProperties.getThread().getPool().getQueueCapacity());
        executor.setThreadNamePrefix("crawlerTaskExecutor-");

        // 设置默认的拒绝策略 线程池和任务队列都已满，新的任务尝试提交时 抛异常
        RejectedExecutionHandler rejectedExecutionHandler = new AbortPolicy();
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);

        executor.initialize();
        return executor;
    }


    @Bean(name = "proxyThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor proxyThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(crawlerProperties.getProxy().getCorePoolSize());
        executor.setMaxPoolSize(crawlerProperties.getProxy().getMaxPoolSize());
        executor.setQueueCapacity(crawlerProperties.getProxy().getQueueCapacity());
        executor.setThreadNamePrefix("proxyTaskPool-");

        // 设置默认的拒绝策略 线程池和任务队列都已满，新的任务尝试提交时 抛异常
        RejectedExecutionHandler rejectedExecutionHandler = new AbortPolicy();
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);

        executor.initialize();
        return executor;
    }

}
