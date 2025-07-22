package com.poly.crawler.config;

import com.poly.crawler.enums.CrawlerEnum;
import com.poly.crawler.process.CrawlerProcessor;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import org.springframework.context.event.EventListener;

@Configuration
@Slf4j
public class CrawlerConfig {

    private final ApplicationContext applicationContext;

    public CrawlerConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private final Map<String, CrawlerProcessor<?, ?, ?>> processorMap = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        for (CrawlerEnum crawlerEnum : CrawlerEnum.values()) {
            try {
                // 使用Spring上下文获取Bean实例
                CrawlerProcessor<?, ?, ?> processor = applicationContext.getBean(crawlerEnum.getClazz());
                processorMap.put(crawlerEnum.getCode(), processor);
                log.info("Successfully loaded processor for crawler: {}", crawlerEnum.getCode());
            } catch (Exception e) {
                // 处理找不到Bean的情况
                log.error("Failed to get bean for class: {}", crawlerEnum.getClazz());
            }
        }
    }

    @Bean(name = "crawlerProcessorMap")
    public Map<String, CrawlerProcessor<?, ?, ?>> crawlerProcessorMap() {
        return processorMap;
    }
}
