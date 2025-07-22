package com.poly.crawler.properties;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CrawlerProperties 类描述
 *
 * @author guojund
 * @version 2025/2/17
 * @since 2025-02-17
 */
@Component
@ConfigurationProperties(prefix = "crawler")
@Data
public class CrawlerProperties {

    private Proxy proxy;
    private Http http;
    private Thread thread;

    private int maxRetries;

    /**
     * 请求间隔时间
     */
    private long retryDelayMs;

    /**
     * 批量请求超时时间
     */
    private int batchTimeoutSeconds;

    /**
     * 单个请求超时时间
     */
    private int timeoutSeconds;


    // Inner classes for nested properties

    @Setter
    @Getter
    public static class Proxy {
        private String host;
        private int port;
        private String userName;
        private String password;

        // Getters and Setters

    }

    @Setter
    @Getter
    public static class Http {
        private int connectionRequestTimeout;
        private int connectTimeout;
        private int socketTimeout;
        private int maxTotalConnect;
        private int maxConnectPerRoute;

        // Getters and Setters

    }

    @Setter
    @Getter
    public static class Thread {
        private Pool pool;

        // Getters and Setters

        @Setter
        @Getter
        public static class Pool {
            private int corePoolSize;
            private int maxPoolSize;
            private int queueCapacity;

            // Getters and Setters

        }
    }
}

