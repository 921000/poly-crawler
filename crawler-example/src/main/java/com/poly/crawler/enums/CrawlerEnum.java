package com.poly.crawler.enums;

import com.poly.crawler.exception.CrawlerException;
import com.poly.crawler.process.CrawlerProcessor;
import com.poly.crawler.processor.dynamic.BaiduBaiKeProcessor;
import com.poly.crawler.processor.dynamic.TencentChildrenRankProcessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * CrawlerEnum 爬虫枚举 维护所有的实现类
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Getter
public enum CrawlerEnum {

    SOURCE_RANK_TENCENT_CHILDREN_RANK("SOURCE_RANK_TENCENT_CHILDREN_RANK", "腾讯儿童榜", "", TencentChildrenRankProcessor.class, 100),
    BAIDU_BAIKE_PROCESSOR("BAIDU_BAIKE_PROCESSOR", "百度百科数据", "", BaiduBaiKeProcessor.class, 100),
    ;

    /**
     * 爬虫编码
     */
    private final String code;

    /**
     * 爬虫名称
     */
    private final String name;

    /**
     * 爬虫url
     */
    private final String url;

    /**
     * 爬虫实现类
     */
    private final Class<? extends CrawlerProcessor<?, ?, ?>> clazz;

    /**
     * 排序
     */
    private final Integer sort;


    CrawlerEnum(String code, String name, String url, Class<? extends CrawlerProcessor<?, ?, ?>> clazz, Integer sort) {
        this.code = code;
        this.name = name;
        this.url = url;
        this.clazz = clazz;
        this.sort = sort;
    }

    /**
     * 通过code 获取 枚举
     */
    public static CrawlerEnum getByCode(String code) {
        for (CrawlerEnum crawlerEnum : CrawlerEnum.values()) {
            if (crawlerEnum.getCode().equals(code)) {
                return crawlerEnum;
            }
        }
        return null;
    }

    /**
     * 通过code 获取 枚举
     */
    public static List<CrawlerEnum> getByCodeList(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new CrawlerException("codes is empty");
        }
        List<CrawlerEnum> crawlerEnums = new ArrayList<>();
        for (CrawlerEnum crawlerEnum : CrawlerEnum.values()) {
            if (codes.contains(crawlerEnum.getCode())) {
                crawlerEnums.add(crawlerEnum);
            }
        }
        if (crawlerEnums.isEmpty()) {
            throw new CrawlerException("codes is not exist");
        }
        return crawlerEnums.stream().sorted(Comparator.comparingInt(CrawlerEnum::getSort)).collect(Collectors.toList());
    }

}
