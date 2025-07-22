package com.poly.crawler.process;

import com.poly.crawler.exception.CrawlerRetryException;
import com.poly.crawler.manager.ProxyHttpManager;
import com.poly.crawler.model.CrawlerContext;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * DefaultAbsJsoupProcessor 静态页面处理类 （默认使用代理处理）
 *
 * @author guojund
 * @version 2024/12/30
 * @since 2024-12-30
 */
@Slf4j
public abstract class DefaultAbsJsoupProcessor<I, R> extends AbsCrawlerProcessor<I, Document, R> {

    @Resource
    private ProxyHttpManager proxyHttpManager;

    @Override
    public Document download(CrawlerContext<I, Document> context) {

        String html = proxyHttpManager.get(context.getUrl());

        if (html == null) {
            throw new CrawlerRetryException("异常重试");
        }
        return Jsoup.parse(html);
    }

    @Override
    public abstract R process(CrawlerContext<I, Document> context);

}
