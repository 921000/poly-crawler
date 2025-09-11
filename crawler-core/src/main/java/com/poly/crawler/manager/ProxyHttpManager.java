package com.poly.crawler.manager;

import com.poly.crawler.exception.CrawlerException;
import com.poly.crawler.exception.CrawlerRetryException;
import com.poly.crawler.properties.BrowserProperties;
import com.poly.crawler.properties.CrawlerProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Resource;
import javax.net.ssl.SSLHandshakeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.ChallengeState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 走代理的http请求工具类
 *
 * @author admin
 */
@Component
@Slf4j
public class ProxyHttpManager {

    @Resource
    private CrawlerProperties crawlerProperties;

    @Resource
    private BrowserProperties config;

    // 用于跟踪当前 UserAgent 的索引
    private int userAgentIndex = 0;

    @Resource(name = "crawlerRequestConfig")
    private RequestConfig requestConfig;

    @Resource(name = "proxyThreadPoolTaskExecutor")
    private ThreadPoolTaskExecutor proxyThreadPoolTaskExecutor;


    public String get(String url) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                RequestBuilder requestBuilder = RequestBuilder.get().setUri(encodeUrl(url));
                requestBuilder.setConfig(requestConfig);
                HttpUriRequest httpUriRequest = requestBuilder.build();
                httpUriRequest.addHeader("Referer", "https://www.google.com/");
                httpUriRequest.addHeader("User-Agent", getRandomUserAgent());
                httpUriRequest.addHeader("Accept-Encoding", null);
                HttpClientContext httpContext = HttpClientContext.create();
                AuthState authState = new AuthState();
                authState.update(new BasicScheme(ChallengeState.PROXY),
                    new UsernamePasswordCredentials(crawlerProperties.getProxy().getUserName(),
                        crawlerProperties.getProxy().getPassword()));
                httpContext.setAttribute(HttpClientContext.PROXY_AUTH_STATE, authState);

                HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

                try (CloseableHttpResponse response = httpClientBuilder.build().execute(httpUriRequest, httpContext)) {
                    byte[] bytes = EntityUtils.toByteArray(response.getEntity());
                    return new String(bytes);
                }

            } catch (SSLHandshakeException e) {
                log.error("异常重试", e);
                throw new CrawlerRetryException(e.getMessage());
            } catch (Exception e) {
                log.error("ProxyHttpManager 执行get请求异常，请求url：{}，异常信息：", url, e);
                throw new CrawlerException(e.getMessage());
            } finally {
                log.info("ProxyHttpManager 执行get请求完成，请求url：{}", url);
            }
        }, proxyThreadPoolTaskExecutor);

        try {
            return future.get(crawlerProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("请求超时，请求url：{}", url, e);
            // 取消任务并中断
            future.cancel(true);
            throw new CrawlerRetryException("请求超时");
        } catch (ExecutionException e) {
            log.error("ProxyHttpManager 执行get请求异常，请求url：{}，异常信息：", url, e);
            throw new CrawlerException(e.getMessage());
        } catch (InterruptedException e) {
            log.error("请求被中断，请求url：{}", url, e);
            // 重新设置中断状态
            Thread.currentThread().interrupt();
            throw new CrawlerException("请求被中断");
        }
    }

    /**
     * 下载图片到指定路径
     *
     * @param url       图片URL
     * @param savePath  保存路径
     * @param useProxy  是否使用代理
     * @throws CrawlerException 爬虫异常
     */
    public void downloadImage(String url, String savePath, boolean useProxy) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                RequestBuilder requestBuilder = RequestBuilder.get().setUri(encodeUrl(url));
                requestBuilder.setConfig(requestConfig);
                HttpUriRequest httpUriRequest = requestBuilder.build();
                httpUriRequest.addHeader("Referer", "https://www.google.com/");
                httpUriRequest.addHeader("User-Agent", getRandomUserAgent());
                httpUriRequest.addHeader("Accept-Encoding", null);

                HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
                HttpClientContext httpContext = null;

                // 如果需要使用代理，则设置代理认证信息
                if (useProxy) {
                    httpContext = HttpClientContext.create();
                    AuthState authState = new AuthState();
                    authState.update(new BasicScheme(ChallengeState.PROXY),
                        new UsernamePasswordCredentials(crawlerProperties.getProxy().getUserName(),
                            crawlerProperties.getProxy().getPassword()));
                    httpContext.setAttribute(HttpClientContext.PROXY_AUTH_STATE, authState);
                }

                try (CloseableHttpResponse response = httpClientBuilder.build().execute(httpUriRequest, httpContext)) {
                    // 获取图片数据
                    byte[] imageData = EntityUtils.toByteArray(response.getEntity());

                    // 保存图片到指定路径
                    java.nio.file.Path path = java.nio.file.Paths.get(savePath);
                    java.nio.file.Files.createDirectories(path.getParent());
                    java.nio.file.Files.write(path, imageData);
                }
            } catch (SSLHandshakeException e) {
                log.error("异常重试", e);
                throw new CrawlerRetryException(e.getMessage());
            } catch (Exception e) {
                log.error("ProxyHttpManager 下载图片异常，请求url：{}，保存路径：{}，异常信息：", url, savePath, e);
                throw new CrawlerException(e.getMessage());
            }
        }, proxyThreadPoolTaskExecutor);

        try {
            future.get(crawlerProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("下载图片请求超时，请求url：{}，保存路径：{}", url, savePath, e);
            // 取消任务并中断
            future.cancel(true);
            throw new CrawlerRetryException("请求超时");
        } catch (ExecutionException e) {
            log.error("ProxyHttpManager 下载图片异常，请求url：{}，保存路径：{}，异常信息：", url, savePath, e);
            throw new CrawlerException(e.getMessage());
        } catch (InterruptedException e) {
            log.error("下载图片请求被中断，请求url：{}，保存路径：{}", url, savePath, e);
            // 重新设置中断状态
            Thread.currentThread().interrupt();
            throw new CrawlerException("请求被中断");
        }
    }

    /**
     * 带重试机制的图片下载方法
     *
     * @param imageUrl   图片URL
     * @param savedFilePath 保存文件路径
     * @param useProxy   是否使用代理
     * @param maxRetries 最大重试次数
     * @return 是否下载成功
     */
    public boolean downloadImageWithRetry(String imageUrl, String savedFilePath, boolean useProxy, int maxRetries) {
        int retryCount = 0;
        while (retryCount <= maxRetries) {
            try {
                this.downloadImage(imageUrl, savedFilePath, useProxy);
                log.info("图片下载成功，URL: {}, 路径: {}", imageUrl, savedFilePath);
                return true;
            } catch (Exception e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    log.warn("图片下载失败，URL: {}，正在进行第{}次重试，异常: {}", imageUrl, retryCount, e.getMessage());
                    try {
                        // 添加延迟重试，避免过于频繁的重试
                        Thread.sleep(1000L * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试过程中线程被中断", ie);
                        return false;
                    }
                } else {
                    log.error("图片下载失败，已达到最大重试次数，URL: {}", imageUrl, e);
                    return false;
                }
            }
        }
        return false;
    }

    private String getRandomUserAgent() {
        if (config.getUserAgents() == null || config.getUserAgents().isEmpty()) {
            throw new IllegalStateException("UserAgent list is empty or not initialized.");
        }
        String userAgent = config.getUserAgents().get(userAgentIndex);
        // 更新索引，实现循环
        userAgentIndex = (userAgentIndex + 1) % config.getUserAgents().size();
        return userAgent;
    }

    private String encodeUrl(String url) throws Exception {
        // 分割URL和查询参数
        int queryStartIndex = url.indexOf('?');
        if (queryStartIndex == -1) {
            // 没有查询参数，直接返回
            return url;
        }

        String baseUrl = url.substring(0, queryStartIndex);
        String queryParams = url.substring(queryStartIndex + 1);

        // 解析查询参数并编码
        StringBuilder encodedQueryParams = new StringBuilder();
        for (String param : queryParams.split("&")) {
            String[] keyValue = param.split("=", 2);
            String key = URLEncoder.encode(keyValue[0], StandardCharsets.UTF_8.toString());
            String value = keyValue.length > 1 ? URLEncoder.encode(keyValue[1], StandardCharsets.UTF_8.toString()) : "";
            if (encodedQueryParams.length() > 0) {
                encodedQueryParams.append("&");
            }
            encodedQueryParams.append(key).append("=").append(value);
        }

        return baseUrl + "?" + encodedQueryParams;
    }
}
