package com.poly.crawler.manager;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Playwright.CreateOptions;
import com.poly.crawler.exception.CrawlerException;
import com.poly.crawler.properties.BrowserProperties;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 该类负责管理 Playwright 实例、浏览器实例和浏览器上下文。
 * - 创建和关闭 Playwright 实例
 * - 启动和关闭浏览器实例
 * - 创建和关闭浏览器上下文
 * - 提供创建新页面的方法
 *
 * @author guojund
 * @version 2024/12/30
 * @since 2024-12-30
 */
@Component
@Slf4j
public class PlaywrightManager implements InitializingBean, DisposableBean {

    //  定义一个全局变量来存储当前的索引值
    private static final AtomicInteger globalIndex = new AtomicInteger(0);
    private final List<BrowserContext> browserContexts;
    private final Map<BrowserContext, Browser> browserMap;
    private final Map<BrowserContext, Playwright> playwrightMap;
    private final int maxCount;
    private final BrowserProperties config;
    private final Semaphore semaphore;
    private int userAgentIndex = 0; // 用于跟踪当前 UserAgent 的索引

    public PlaywrightManager(BrowserProperties config) {
        this.config = config;
        this.browserContexts = new CopyOnWriteArrayList<>();
        this.browserMap = new ConcurrentHashMap<>();
        this.playwrightMap = new ConcurrentHashMap<>();
        this.maxCount = config.getPoolSize();
        this.semaphore = new Semaphore(maxCount);
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Creating Playwright instance and launching browser.");

        Map<String, String> env = new HashMap<>();
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1");
        CreateOptions options = new CreateOptions();
        options.setEnv(env);

        for (int i = 0; i < maxCount; i++) {
            Playwright playwright = Playwright.create(options);
            // 启动 Chromium 浏览器
            LaunchOptions launchOptions = new LaunchOptions();
            launchOptions.setExecutablePath(Paths.get(config.getExecutablePath()));
            launchOptions.setHeadless(config.isHeadless());
            Browser browser = playwright.chromium().launch(launchOptions);
            BrowserContext context = browser.newContext();
            browserContexts.add(context);
            browserMap.put(context, browser);
            playwrightMap.put(context, playwright);
        }
        log.info("Playwright instance and browser launched successfully.count:[{}]", browserContexts.size());
    }

    @Override
    public void destroy() {
        log.info("Closing browser.");
        if (browserContexts != null && !browserContexts.isEmpty()) {
            browserContexts.forEach(browserContext -> {
                browserContext.close();
                Browser browser = browserMap.remove(browserContext);
                if (browser != null) {
                    browser.close();
                }
                Playwright playwright = playwrightMap.remove(browserContext);
                if (playwright != null) {
                    playwright.close();
                }
            });
        }
    }

    public Page getPage() {
        try {
            semaphore.acquire(); // 获取信号量，控制最大页面数量
        } catch (InterruptedException e) {
            log.error("Error acquiring semaphore: {}", e.getMessage());
            throw new CrawlerException(e);
        }
        return createPage();
    }

    public void returnPage(Page page) {
        if (page != null && !page.isClosed()) {
            page.close();
        }
        semaphore.release(); // 释放信号量
    }

    private synchronized Page createPage() {
        log.info("Creating new page.");
        // 按顺序触发 url
        String userAgent = getRandomUserAgent();
        Map<String, String> headers = new HashMap<>(2);
        headers.put("User-Agent", userAgent);

        // 获取当前浏览器上下文
        BrowserContext browserContext;
        do {
            int index = getIndex();
            log.info("index:{}", index);
            browserContext = browserContexts.get(index);
        } while (!browserContext.pages().isEmpty());

        browserContext.setExtraHTTPHeaders(headers);
        // 创建新页面
        Page page = browserContext.newPage();
        if (page != null && !page.isClosed()) {
            return page;
        }
        // 创建失败 释放信号量
        semaphore.release();
        throw new CrawlerException("Failed to create a new page.");
    }

    private String getRandomUserAgent() {
        if (config.getUserAgents() == null || config.getUserAgents().isEmpty()) {
            throw new IllegalStateException("UserAgent list is empty or not initialized.");
        }
        String userAgent = config.getUserAgents().get(userAgentIndex);
        userAgentIndex = (userAgentIndex + 1) % config.getUserAgents().size(); // 更新索引，实现循环
        return userAgent;
    }

    private int getIndex() {
        int currentIndex;
        int newIndex;
        do {
            currentIndex = globalIndex.get();
            newIndex = (currentIndex + 1) % maxCount;
        } while (!globalIndex.compareAndSet(currentIndex, newIndex));

        return newIndex;
    }
}
