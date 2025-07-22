package com.poly.crawler.config;

import com.poly.crawler.properties.CrawlerProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

/**
 * restTemplate初始化
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Slf4j
@Configuration
public class CrawlerRestTemplateConfig {

    @Resource
    private CrawlerProperties crawlerProperties;

    @Bean(name = "crawlerRequestConfig")
    public RequestConfig requestConfig() {
        return RequestConfig.custom()
            .setConnectTimeout(crawlerProperties.getHttp().getConnectTimeout())
            .setSocketTimeout(crawlerProperties.getHttp().getSocketTimeout())
            .setConnectionRequestTimeout(crawlerProperties.getHttp().getConnectionRequestTimeout())
            .setProxy(new HttpHost(crawlerProperties.getProxy().getHost(), crawlerProperties.getProxy().getPort()))
            .build();

    }

    @Bean(name = "crawlerRestTemplate")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = null;
        try {
            restTemplate = new RestTemplate(this.createFactory());
            handleMessageConverters(restTemplate);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            log.error("restTemplate构建失败", e);
        }

        return restTemplate;
    }

    /**
     * 通过apache httpClient 实现restTemplate连接池
     */
    @NonNull
    private ClientHttpRequestFactory createFactory()
        throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        // 安全套接字配置跳过SSL认证
        SSLContext sslContext = SSLContextBuilder.create()
            .setProtocol(SSLConnectionSocketFactory.SSL)
            .loadTrustMaterial((x, y) -> true)
            .build();

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            // https跳过ssl认证
            .register("https", new SSLConnectionSocketFactory(sslContext, (x, y) -> true))
            .build();

        // 配置超时时间
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(crawlerProperties.getHttp().getConnectTimeout())
            .setSocketTimeout(crawlerProperties.getHttp().getSocketTimeout())
            .setConnectionRequestTimeout(crawlerProperties.getHttp().getConnectionRequestTimeout())
            .build();

        // 配置连接池
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(crawlerProperties.getHttp().getMaxTotalConnect());
        cm.setDefaultMaxPerRoute(crawlerProperties.getHttp().getMaxConnectPerRoute());

        HttpClient httpClient = HttpClientBuilder.create()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(config)
            .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }


    /**
     * 处理数据转换
     */
    private void handleMessageConverters(@NonNull RestTemplate restTemplate) {
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();

        messageConverters.removeIf(converter ->
            converter instanceof StringHttpMessageConverter ||
                converter instanceof GsonHttpMessageConverter ||
                converter instanceof MappingJackson2HttpMessageConverter
        );

        // 默认字符集由ISO_8859_1改为UTF_8
        messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        messageConverters.add(CrawlerJsonConfig.getFastJsonHttpMessageConverter());
    }
}
