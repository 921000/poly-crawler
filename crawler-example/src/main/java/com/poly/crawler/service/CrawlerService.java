package com.poly.crawler.service;

import com.poly.crawler.enums.CrawlerEnum;
import com.poly.crawler.exception.CrawlerException;
import com.poly.crawler.model.CrawlerContext;
import com.poly.crawler.process.AbsCrawlerProcessor;
import com.poly.crawler.process.CrawlerProcessor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * CrawlerService 调度和管理不同类型的爬虫任务
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Service
public class CrawlerService {


    @Autowired
    @Qualifier("crawlerProcessorMap")
    private Map<String, CrawlerProcessor<?,?,?>> crawlerProcessorMap;


    /**
     * 爬取单条数据( 自定义Context)
     */
    @SuppressWarnings("unchecked")
    public <I, O, R>  R fetch(String code,CrawlerContext<I, O> context) {
        AbsCrawlerProcessor<I, O, R> processor = (AbsCrawlerProcessor<I, O, R>) crawlerProcessorMap.get(code);
        if (processor != null) {
            return processor.execute(context);
        } else {
            throw new CrawlerException("No processor found for code: " + code);
        }
    }

    /**
     * 爬取 单条 数据
     */
    @SuppressWarnings("unchecked")
    public <I, O, R>  R fetch(String code) {
        AbsCrawlerProcessor<I, O, R> processor = (AbsCrawlerProcessor<I, O, R>) crawlerProcessorMap.get(code);
        if (processor != null) {
            CrawlerContext<I, O> context = processor.addCrawlerContext();
            return processor.execute(context);
        } else {
            throw new CrawlerException("No processor found for code: " + code);
        }
    }

    /**
     * 同一类型 批量 爬取 数据
     */
    @SuppressWarnings("unchecked")
    public <I, O, R>  List<R> fetchBatch(String code) {
        AbsCrawlerProcessor<I, O, R> processor = (AbsCrawlerProcessor<I, O, R>) crawlerProcessorMap.get(code);
        if (processor != null) {
            return processor.executeBatch();
        } else {
            throw new CrawlerException("No processor found for code: " + code);
        }
    }


    /**
     * 不同枚举类型 批量 爬取 数据
     */
    public List<Object> fetchBatch(List<String> codeList) {
        if (codeList.isEmpty()) {
            throw new CrawlerException("code is empty");
        }
        return codeList.stream().map(this::fetch).collect(Collectors.toList());
    }

    /**
     * 总分 的触发形式，先 定义 父节点 返回 list，子节点 单个处理 （子节点可以有多个，每个子节点 定义清楚 要拿哪个url）
     */
    public <R> List<R> fetch(List<String> codes) {
        // 获取 code 对应的枚举信息
        List<CrawlerEnum> crawlEnumList =  CrawlerEnum.getByCodeList(codes);
        // 只处理一个code
        if (crawlEnumList.size() == 1) {
            return fetch(crawlEnumList.get(0).getCode());
        }
        // 第一次 调度
        List<R> currentResult = fetch(crawlEnumList.get(0).getCode());

        // 从 1 开始 到 crawlEnumList.size()
        for (int i = 1; i < crawlEnumList.size(); i++) {
            String code = crawlEnumList.get(i).getCode();
            currentResult = fetchBatch(code, currentResult);
        }
        // code 列表，上次 输出 作为下次的输入
        return currentResult;
    }

    @SuppressWarnings("unchecked")
    private <I, O, R> List<R> fetchBatch(String code,List<R> results) {
        AbsCrawlerProcessor<I, O, R> processor = (AbsCrawlerProcessor<I, O, R>) crawlerProcessorMap.get(code);
        if (processor != null) {
            return processor.executeBatchByLastResult(results);
        } else {
            throw new CrawlerException("No processor found for code: " + code);
        }
    }

}
