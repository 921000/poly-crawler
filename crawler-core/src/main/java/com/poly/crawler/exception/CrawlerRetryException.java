package com.poly.crawler.exception;

/**
 * 爬虫重试异常
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
public class CrawlerRetryException extends RuntimeException {

    /**
     * 默认构造函数
     */
    public CrawlerRetryException() {
        super();
    }

    /**
     * 带有消息的构造函数
     *
     * @param message 异常消息
     */
    public CrawlerRetryException(String message) {
        super(message);
    }

    /**
     * 带有消息和原因的构造函数
     *
     * @param message 异常消息
     * @param cause   异常的原因
     */
    public CrawlerRetryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 带有原因的构造函数
     *
     * @param cause 异常的原因
     */
    public CrawlerRetryException(Throwable cause) {
        super(cause);
    }
}

