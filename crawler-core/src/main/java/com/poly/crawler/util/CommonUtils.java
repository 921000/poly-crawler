package com.poly.crawler.util;

import java.util.Random;

/**
 * CommonUtils 类描述
 *
 * @author guojund
 * @version 2025/1/20
 * @since 2025-01-20
 */
public class CommonUtils {


    public static void randomSleep(int minSeconds, int maxSeconds) {
        Random random = new Random();
        // 生成 随机数
        int sleepTime = random.nextInt(maxSeconds - minSeconds + 1) + minSeconds;
        try {
            Thread.sleep(sleepTime * 1000L); // 将秒转换为毫秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread was interrupted, Failed to complete operation");
        }
    }

}
