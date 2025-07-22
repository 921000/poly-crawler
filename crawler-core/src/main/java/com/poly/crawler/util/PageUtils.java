package com.poly.crawler.util;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;

/**
 * PageUtils page 类操作 工具类
 *
 * @author guojund
 * @version 2025/1/3
 * @since 2025-01-03
 */
@Slf4j
public class PageUtils {


    /**
     * 滚动页面到底部（间隔 300 毫秒）
     */
    public static void scrollPageToBottom(Page page) {
        page.evaluate("() => {\n" +
                "    return new Promise((resolve, reject) => {\n" +
                "        const totalHeight = document.documentElement.scrollHeight;\n" +
                "        let distance = 500;\n" +
                "        let lastScrollY = 0;\n" +
                "        let noChangeCount = 0;\n" +
                "        const maxNoChangeCount = 5; // 设置最大连续无变化次数\n" +
                "\n" +
                "        let timer = setInterval(() => {\n" +
                "            window.scrollBy(0, distance);\n" +
                "            let currentScrollY = window.scrollY;\n" +
                "\n" +
                "            if (currentScrollY === lastScrollY) {\n" +
                "                noChangeCount++;\n" +
                "                if (noChangeCount >= maxNoChangeCount) {\n" +
                "                    clearInterval(timer);\n" +
                "                    resolve();\n" +
                "                }\n" +
                "            } else {\n" +
                "                noChangeCount = 0;\n" +
                "            }\n" +
                "\n" +
                "            lastScrollY = currentScrollY;\n" +
                "        }, 300);\n" +
                "    });\n" +
                "}");
    }


    /**
     * 监听弹窗并关闭弹窗
     */
    public static void handleDialogs(Page page) {
        page.onDialog(dialog -> {
            log.info("Dialog message: {}", dialog.message());
            dialog.dismiss();
        });
    }
}
