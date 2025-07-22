package com.poly.crawler.processor.dynamic;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.poly.crawler.model.CrawlerContext;
import com.poly.crawler.model.SourceRank;
import com.poly.crawler.process.DefaultAbsPlayWrightProcessor;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TencentChildrenRankProcessor 类描述
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Service
@Slf4j
public class TencentChildrenRankProcessor extends DefaultAbsPlayWrightProcessor<String,List<SourceRank>> {

    @Override
    protected void beforeNavigateUrl(Page page) {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    @Override
    public List<SourceRank> process(CrawlerContext<String,Page> context) {
        Page page = context.getOutput();
        List<SourceRank> sourceRanks = new ArrayList<>();
        // 获取所有符合条件的 div 元素
        List<ElementHandle> items = page.querySelectorAll("div.item.item_a");

        for (ElementHandle item : items) {
            try {
                SourceRank sourceRank = new SourceRank();
                // 获取 span.num 的文本内容
                ElementHandle spanNum = item.querySelector("span.num");
                String numValue = spanNum.innerText();
                sourceRank.setRankNum(Integer.valueOf(numValue));

                // 获取 a.name 的文本内容
                ElementHandle aName = item.querySelector("a.name");
                String aText = aName.innerText();
                sourceRank.setName(aText);

                // 获取 a.name 的 href 属性值
                String hrefValue = aName.getAttribute("href");
                sourceRank.setUrl(hrefValue);
                sourceRanks.add(sourceRank);
            } catch (Exception e) {
                log.error("获取元素值时出错: {}", e.getMessage());
            }
        }

        return sourceRanks;
    }

    @Override
    public CrawlerContext<String, Page> addCrawlerContext() {
        CrawlerContext<String, Page> context =  new CrawlerContext<String, Page>();
        context.setUrl("https://v.qq.com/biu/ranks/?t=hotsearch&channel=106");
        return context;
    }
}
