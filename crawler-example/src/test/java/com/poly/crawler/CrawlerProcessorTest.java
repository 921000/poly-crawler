package com.poly.crawler;

import com.poly.crawler.enums.CrawlerEnum;
import com.poly.crawler.model.SourceRank;
import com.poly.crawler.service.CrawlerService;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * TencentProcessorTest 类描述
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class CrawlerProcessorTest {


    @Autowired
    private CrawlerService crawlerService;

    @Test
    public void test() {

        List<SourceRank> result =crawlerService.fetch(CrawlerEnum.SOURCE_RANK_TENCENT_CHILDREN_RANK.getCode());
        result.forEach(System.out::println);
    }


    @Test
    public void test01() {

        Map<String,String> result = crawlerService.fetch(CrawlerEnum.BAIDU_BAIKE_PROCESSOR.getCode());
        result.forEach((k,v)->System.out.println(k+"--"+v));
    }

}
