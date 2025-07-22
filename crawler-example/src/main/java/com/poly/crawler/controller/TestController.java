package com.poly.crawler.controller;

import com.poly.crawler.service.CrawlerService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TestController 测试类
 *
 * @author guojund
 * @version 2024/12/30
 * @since 2024-12-30
 */
@RestController
public class TestController {


    @Autowired
    private CrawlerService crawlerService;

    @GetMapping(value = "test")
    public ResponseEntity<?> test(@RequestParam(value = "code", required = false) String code) throws Exception {
        return ResponseEntity.ok(crawlerService.fetch(code));
    }

    @GetMapping(value = "testBatch")
    public ResponseEntity<?> testBatch(@RequestParam(value = "code", required = false) List<String> codes) throws Exception {
        return ResponseEntity.ok(crawlerService.fetch(codes));
    }
}
