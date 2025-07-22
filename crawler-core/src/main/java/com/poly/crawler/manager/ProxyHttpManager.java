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

    private int userAgentIndex = 0; // 用于跟踪当前 UserAgent 的索引

    @Resource(name = "crawlerRequestConfig")
    private RequestConfig requestConfig;

    @Resource
    private ThreadPoolTaskExecutor crawlerTaskExecutor;


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
            }
        },crawlerTaskExecutor);

        try {
            return future.get(crawlerProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("请求超时，请求url：{}", url, e);
            future.cancel(true); // 取消任务并中断
            throw new CrawlerRetryException("请求超时");
        } catch (ExecutionException e) {
            log.error("ProxyHttpManager 执行get请求异常，请求url：{}，异常信息：", url, e);
            throw new CrawlerException(e.getMessage());
        } catch (InterruptedException e) {
            log.error("请求被中断，请求url：{}", url, e);
            Thread.currentThread().interrupt(); // 重新设置中断状态
            throw new CrawlerException("请求被中断");
        }
    }

    private String getRandomUserAgent() {
        if (config.getUserAgents() == null || config.getUserAgents().isEmpty()) {
            throw new IllegalStateException("UserAgent list is empty or not initialized.");
        }
        String userAgent = config.getUserAgents().get(userAgentIndex);
        userAgentIndex = (userAgentIndex + 1) % config.getUserAgents().size(); // 更新索引，实现循环
        return userAgent;
    }

    private String encodeUrl(String url) throws Exception {
        // 分割URL和查询参数
        int queryStartIndex = url.indexOf('?');
        if (queryStartIndex == -1) {
            return url; // 没有查询参数，直接返回
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
