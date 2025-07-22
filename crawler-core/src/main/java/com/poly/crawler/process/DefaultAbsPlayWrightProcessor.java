package com.poly.crawler.process;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;

import com.poly.crawler.exception.CrawlerRetryException;
import com.poly.crawler.manager.PlaywrightManager;
import com.poly.crawler.model.CrawlerContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultAbsPlayWrightProcessor 动态页面处理类
 *
 * @author guojund
 * @version 2024/12/30
 * @since 2024-12-30
 */
@Slf4j
public abstract class DefaultAbsPlayWrightProcessor<I, R> extends AbsCrawlerProcessor<I, Page, R> {

    @Resource
    private PlaywrightManager playbackManager;

    @Override
    public Page download(CrawlerContext<I, Page> context) {
        Page page = context.getOutput();
        if (page == null) {
            page = playbackManager.getPage();
            context.setOutput(page);
            // 监听事件，监听请求 或者 监听事件，监听页面加载完成
            beforeNavigateUrl(page);
            log.info("Downloading page: {}", page);
            // 打开目标 URL
            String url = context.getUrl();
            log.info("Navigating to URL: {}", url);
            page.navigate(url);
        } else {
            page.reload();
        }

        // 等待页面完全加载的方法
        log.info("Waiting for page to load completely.");
        afterNavigateUrl(page);
        return page;
    }

    /**
     * 页面加载之前  （监听、请求响应，滑动页面等 ）
     */
    protected void beforeNavigateUrl(Page page) {
        // default do nothing
    }


    /**
     * 导航 到url 之后 需要做的事情 一般为等待页面加载
     * 页面加载方式，默认为所有网络请求响应完成。
     * 支持的加载方式：
     * page.waitForLoadState
     * - {@code DOMCONTENTLOADED}：初始的 HTML 文档已被完全加载和解析，但不必等待样式表、图像和子框架的加载完成。
     * - {@code LOAD}：页面的所有资源（包括框架、图像、样式表等）都已加载完成。
     * - {@code NETWORKIDLE}：所有网络连接（如 fetch、xhr 等）都已完成，且没有未解析的资源需要处理（例如，CSS、图像等）。
     * page.waitForNavigation()  等待页面导航完成的方法 可以指定特定元素 或者 连接，请求加载完成
     */

    protected void afterNavigateUrl(Page page) {
        log.info("Waiting for page to load with state: {}", LoadState.NETWORKIDLE);

        page.waitForLoadState(LoadState.NETWORKIDLE);
    }


    @Override
    public abstract R process(CrawlerContext<I, Page> context);

    @Override
    public List<Class<? extends Exception>> retryExceptions() {
        return Stream.of(TimeoutError.class, CrawlerRetryException.class).collect(Collectors.toList());
    }

    @Override
    public void after(CrawlerContext<I, Page> context, R result) {
        log.info("AbstractPageProcessor: Returning page to pool: {}", context);
        playbackManager.returnPage(context.getOutput());
    }
}
