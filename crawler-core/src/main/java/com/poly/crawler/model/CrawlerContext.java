package com.poly.crawler.model;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CrawlerContext 是一个用于存储爬虫任务上下文信息的类。
 * 它包含了爬虫任务的请求地址（单个或多个）以及其他可能需要的输入参数。
 * 该类提供了多个静态工厂方法来创建 CrawlerContext 实例，并且通过私有构造函数防止外部直接实例化。
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CrawlerContext<I,O> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求地址
     */
    private String url;

    /**
     * 输入参数
     */
    private I params;

    /**
     * 输出结果
     */
    private O output;

    /**
     * 扩展参数
     */
    private ConcurrentHashMap<String, Object> extMap;

}
