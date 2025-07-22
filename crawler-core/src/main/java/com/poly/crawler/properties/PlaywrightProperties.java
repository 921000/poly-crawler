package com.poly.crawler.properties;


import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * PlaywrightConfig 动态浏览器 配置类
 *
 * @author guojund
 * @version 2024/12/30
 * @since 2024-12-30
 */
@Component
@ConfigurationProperties(prefix = "playwright")
@Data
public class PlaywrightProperties {

    private boolean headless;
    private List<String> userAgents;
    private Integer poolSize;


}
