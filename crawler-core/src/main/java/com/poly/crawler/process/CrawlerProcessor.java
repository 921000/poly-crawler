package com.poly.crawler.process;

import com.poly.crawler.model.CrawlerContext;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CrawlerProcessor 是一个用于定义爬虫处理逻辑的接口。
 * 定义了爬虫任务的各个阶段，包括获取URL、下载页面内容、处理页面结果以及确定需要重试的异常。
 * 该接口使用泛型参数 I 表示输入参数类型，O 表示页面输出类型，R 表示格式化后的最终结果类型。
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
public interface CrawlerProcessor<I, O, R> {

    /**
     * 参数构造
     */
    CrawlerContext<I, O> addCrawlerContext();

    /**
     * 批量处理 参数构造
     */
    CopyOnWriteArrayList<CrawlerContext<I, O>> addAllCrawlerContext();

    /**
     * 基于 上一次的结果，生成批量处理 context
     */
    CopyOnWriteArrayList<CrawlerContext<I, O>> addAllCrawlerContext(List<R> result);

    /**
     * 下载页面内容
     */
    O download(CrawlerContext<I, O> context);

    /**
     * 处理页面结果
     */
    R process(CrawlerContext<I, O> context);

    /**
     * 需要重试的异常
     */
    List<Class<? extends Exception>> retryExceptions();

    /**
     * before
     */
    void before(CrawlerContext<I, O> context);

    /**
     * after
     */
    void after(CrawlerContext<I, O> context, R result);

}
