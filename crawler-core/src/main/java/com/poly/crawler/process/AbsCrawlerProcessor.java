package com.poly.crawler.process;

import com.alibaba.fastjson.JSON;
import com.poly.crawler.exception.CrawlerException;
import com.poly.crawler.exception.CrawlerRetryException;
import com.poly.crawler.model.CrawlerContext;
import com.poly.crawler.properties.CrawlerProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AbsCrawlerProcessor 是一个抽象类，实现了 CrawlerProcessor 接口，提供了爬虫处理的基本功能。 该类封装了通用的爬虫逻辑，包括下载页面内容、处理页面结果、重试机制以及批量处理等。
 * 子类需要实现具体的下载和处理逻辑。
 *
 * <p>主要功能：
 * <ul>
 *     <li>执行单个爬虫任务：通过 {@link #execute(CrawlerContext)} 方法启动单个爬虫任务。</li>
 *     <li>执行批量爬虫任务：通过 {@link #executeBatch()} 方法启动批量爬虫任务，并支持线程池并发处理。</li>
 *     <li>带重试机制的下载：通过 {@link #downloadWithRetry(CrawlerContext, List)} 方法实现下载失败时的自动重试。</li>
 * </ul>
 *
 * <p>配置项：
 * <ul>
 *     <li>{@code crawler.maxRetries}：最大重试次数，默认为3次。</li>
 *     <li>{@code crawler.retryDelayMs}：请求间隔时间，默认为2000毫秒（2秒）。</li>
 *     <li>{@code crawler.batchTimeoutSeconds}：批量请求超时时间，默认为300秒（5分钟）。</li>
 * </ul>
 *
 * @param <I> 输入参数类型
 * @param <O> 页面输出类型
 * @param <R> 格式化后的最终结果类型
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Slf4j
public abstract class AbsCrawlerProcessor<I, O, R> implements CrawlerProcessor<I, O, R> {

    @Resource
    protected CrawlerProperties crawlerProperties;

    @Resource(name = "crawlerTaskExecutor")
    protected ThreadPoolTaskExecutor crawlerTaskExecutor;

    /**
     * 批量执行
     */
    public List<R> executeBatch() {
        return this.executeBatch(this.addAllCrawlerContext());
    }

    /**
     * 基于上次执行结果 批量执行
     */
    public List<R> executeBatchByLastResult(List<R> resultList) {
        return this.executeBatch(this.addAllCrawlerContext(resultList));
    }

    /**
     * 执行爬虫处理流程(支持批量处理)
     */
    public List<R> executeBatch(List<CrawlerContext<I, O>> contextList) {
        // 保存线程池执行结果
        List<CompletableFuture<R>> futures = new ArrayList<>();
        for (CrawlerContext<I, O> context : contextList) {
            // 添加间隔时间
            try {
                Thread.sleep(crawlerProperties.getRetryDelayMs());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while waiting for request interval", ie);
            }
            CompletableFuture<R> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 调用 execute 方法处理单个输入
                    return this.execute(context);
                } catch (Exception e) {
                    log.error("BatchCrawlerProcessor error during batch execution: ", e);
                    return null;
                }
            }, crawlerTaskExecutor)
                    .exceptionally(throwable -> {
                        log.error("Error during batch execution: ", throwable);
                        return null;
                    });
            futures.add(future);
        }

        int BATCH_TIMEOUT_SECONDS = crawlerProperties.getBatchTimeoutSeconds();
        // 等待所有任务完成或超时
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allFutures.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Batch processing timed out after {} seconds", BATCH_TIMEOUT_SECONDS);
            allFutures.cancel(true);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during batch processing: ", e);
        }

        // 收集结果
        List<R> results = new ArrayList<>();
        for (CompletableFuture<R> future : futures) {
            if (!future.isCancelled()) {
                try {
                    R result = future.getNow(null);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    log.error("Error retrieving result from future: ", e);
                }
            }
        }
        return results;
    }

    /**
     * 执行爬虫处理流程
     */
    public R execute(CrawlerContext<I, O> context) {
        O output;
        R result = null;
        try {
            this.before(context);
            // 下载
            log.info("AbstractPageProcessor: Starting download for input: {}", context);
            output = downloadWithRetry(context, retryExceptions());
            if (output != null) {
                log.info("AbstractPageProcessor: Download successful for input: {}", context);
                // 处理
                context.setOutput(output);
                result = process(context);
                log.info("AbstractPageProcessor: Processed Content: {}", result);
                return result;
            } else {
                log.warn("AbstractPageProcessor: Failed to download content for input: {}", JSON.toJSONString(context));
            }
        } catch (Exception e) {
            log.error("AbstractPageProcessor error during execution: ", e);
            throw new CrawlerException(e.getMessage());
        } finally {
            this.after(context, result);
        }
        return null;
    }

    @Override
    public abstract O download(CrawlerContext<I, O> context);

    @Override
    public abstract R process(CrawlerContext<I, O> context);

    @Override
    public List<Class<? extends Exception>> retryExceptions() {
        return Stream.of(TimeoutException.class, CrawlerRetryException.class).collect(Collectors.toList());
    }

    @Override
    public void before(CrawlerContext<I, O> context) {
    }

    @Override
    public void after(CrawlerContext<I, O> context, R result) {
    }


    @Override
    public abstract CrawlerContext<I, O> addCrawlerContext();

    @Override
    public CopyOnWriteArrayList<CrawlerContext<I, O>> addAllCrawlerContext(List<R> result) {
        return new CopyOnWriteArrayList<>();
    }


    @Override
    public CopyOnWriteArrayList<CrawlerContext<I, O>> addAllCrawlerContext() {
        return new CopyOnWriteArrayList<>();
    }

    /**
     * 带重试机制的下载方法
     */
    protected O downloadWithRetry(CrawlerContext<I, O> context, List<Class<? extends Exception>> retryExceptions) {
        int retryCount = 0;
        while (retryCount < crawlerProperties.getMaxRetries()) {
            try {
                O result = download(context);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                if (shouldRetryOnException(e, retryExceptions)) {
                    handleRetryException(e, retryCount);
                } else {
                    log.error("Download failed with non-retryable exception: {}", e.getMessage(), e);
                    throw new CrawlerException(e.getMessage());
                }
            }

            retryCount++;
            sleepForRetry();
        }
        log.error("Download failed after {} retries", crawlerProperties.getMaxRetries());
        throw new CrawlerException("Download failed after " + crawlerProperties.getMaxRetries() + " retries");
    }

    /**
     * 判断是否需要重试
     */
    private boolean shouldRetryOnException(Exception e, List<Class<? extends Exception>> retryExceptions) {
        for (Class<? extends Exception> exceptionType : retryExceptions) {
            if (exceptionType.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理重试异常
     */
    private void handleRetryException(Exception e, int retryCount) {
        log.warn("Download failed with exception: {}. Retrying {}/{}", e.getMessage(), retryCount + 1, crawlerProperties.getMaxRetries());
    }

    /**
     * 等待重试
     */
    private void sleepForRetry() {
        try {
            Thread.sleep(crawlerProperties.getRetryDelayMs());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for retry", ie);
        }
    }
}
