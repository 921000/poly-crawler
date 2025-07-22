package com.poly.crawler;


import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import java.util.List;

/**
 * PlaywrightExample 类描述
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */

public class PlaywrightExample {

    public static void main(String[] args) {
        // 创建 Playwright 实例
        try (Playwright playwright = Playwright.create()) {
            // 启动 Chromium 浏览器
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            // 打开目标 URL
            String url = "https://t.youku.com/yep/page/s/hot_search_rank?spm=a2hja.14919748_WEBHOME_NEW.search.d_more";
            page.navigate(url);

            // 等待页面完全加载
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // 获取页面内容
            String content = page.content();
            System.out.println(content);

            // 关闭浏览器
            context.close();
            browser.close();
        }
    }
}

