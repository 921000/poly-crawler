package com.poly.crawler.model;

import lombok.Data;

/**
 * CrawlerRequest 类描述
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Data
public class CrawlerRequest<T> {

    private String code;

    private String url;

    private T param;

}
