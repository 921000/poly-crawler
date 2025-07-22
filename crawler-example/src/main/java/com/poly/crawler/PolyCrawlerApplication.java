package com.poly.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PolyCrawlerApplication 类描述
 *
 * @author guojund
 * @version 2024/12/16
 * @since 2024-12-16
 */
@SpringBootApplication(scanBasePackages = "com.ysten")
public class PolyCrawlerApplication {

    public static void main(String[] args) {

        SpringApplication.run(PolyCrawlerApplication.class, args);
    }

}
