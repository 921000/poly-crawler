package com.poly.crawler.processor.dynamic;

import cn.hutool.core.util.StrUtil;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.poly.crawler.model.CrawlerContext;
import com.poly.crawler.process.DefaultAbsPlayWrightProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * BaiduBaiKeProcessor 百度百科search
 *
 * @author guojund
 * @version 2025/1/2
 * @since 2025-01-02
 */
@Service
@Slf4j
public class BaiduBaiKeProcessor extends DefaultAbsPlayWrightProcessor<String, Map<String,String>> {

    @Override
    protected void afterNavigateUrl(Page page) {
        // 判断
        log.info("Waiting for page to load with state: {}", LoadState.LOAD);
        page.waitForLoadState(LoadState.LOAD);
    }

    @Override
    public Map<String,String> process(CrawlerContext<String, Page> context) {

        Page page = context.getOutput();
        List<ElementHandle> itemNameElements = page.querySelectorAll("xpath=//dt[contains(@class, 'itemName_')]");
        List<ElementHandle> itemValueElements = page.querySelectorAll("xpath=//dd[contains(@class, 'itemValue_')]");

        // 创建一个 Map 来存储中英文映射关系
        Map<String, String> itemMap = new HashMap<>();

        for (int i = 0; i < itemNameElements.size(); i++) {
            String itemName = itemNameElements.get(i).innerText();
            String itemValue = itemValueElements.get(i).innerText();

            // 去除引用标记
            itemValue = itemValue.replaceAll("\\[\\d+]", "").trim();

            itemMap.put(itemName, itemValue);
        }



        // id
        ElementHandle element = page.querySelector("#J-vars");
        // 简介
        itemMap.put("summary",this.getSummary(page));
        // 提取 data-lemmaid 属性值
        String dataLemmaId = element.getAttribute("data-lemmaid");
        itemMap.put("id",dataLemmaId);
        return itemMap;
    }

    /**
     * 获取概要信息
     */
    private String getSummary(Page page) {
        // 判断 是否有 剧情介绍 有在爬取
        ElementHandle categoryElement = page.querySelector("xpath=//div[contains(@class, 'catalogList_')]");
        if (categoryElement == null) {
            return StrUtil.EMPTY;
        }
        List<ElementHandle> elementHandles = categoryElement.querySelectorAll(
                "xpath=//span[contains(@class, 'catalogText_')]");

        if (elementHandles.isEmpty()) {
            return StrUtil.EMPTY;
        }
        if (!elementHandles.get(0).innerText().contains("剧情")) {
            return StrUtil.EMPTY;
        }

        ElementHandle elementHandle = page.querySelector("div[data-idx=\"0-1\"]");
        if (elementHandle == null) {
            return StrUtil.EMPTY;
        }
        List<ElementHandle> summaryElementHandlesList = elementHandle.querySelectorAll("xpath=//span[contains(@class, 'text_')]");
        if (summaryElementHandlesList.isEmpty()) {
            return StrUtil.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        for (ElementHandle handle : summaryElementHandlesList) {
            builder.append(handle.innerText());
        }
        return builder.toString();
    }

    @Override
    public CrawlerContext<String, Page> addCrawlerContext() {

        String url = "http://baike.baidu.com/search/word?word=%s&pic=1&sug=1&enc=utf8";
        CrawlerContext<String, Page> context = new CrawlerContext<String, Page>();
        context.setUrl(String.format(url, "笑傲江湖"));
        return context;
    }

}
