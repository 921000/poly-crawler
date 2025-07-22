package com.poly.crawler.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;

/**
 * BaiduDetailInfo 百度百科 爬取数据详情
 *
 * @author guojund
 * @version 2025/1/8
 * @since 2025-01-08
 */
@Data
public class BaiduDetailInfo {

    private String id;

    private String url;


    /**
     * 概述
     */
    private String summary;

    /**
     * 中文名
     */
    private String chineseTitle;

    /**
     * 片长
     */
    private String duration;

    /**
     * 票房
     */
    private String boxOffice;

    /**
     * 发行公司
     */
    private String distributor;

    /**
     * 在线播放平台|网络播放平台
     */
    private String onlinePlatforms;

    /**
     * IMDb 编码
     */
    private String imdbCode;

    /**
     * 外文名
     */
    private String englishTitle;

    /**
     * 编剧
     */
    private String screenwriter;


    /**
     * 制片人
     */
    private String producer;

    /**
     * 拍摄地点
     */
    private String filmingLocation;

    /**
     * 制片地区
     */
    private String productionRegion;
    /**
     * 主演
     */
    private String mainActors;
    /**
     * 配乐
     */
    private String music;
    /**
     * 色彩
     */
    private String color;
    /**
     * 制片成本
     */
    private String productionCost;
    /**
     * 主要奖项
     */
    private String mainAwards;
    /**
     * 导演
     */
    private String director;
    /**
     * 类型
     */
    private String genre;
    /**
     * 出品公司
     */
    private String productionCompany;
    /**
     * 拍摄日期
     */
    private String filmingDate;
    /**
     * 首播时间|上映时间
     */
    private String releaseDate;
    /**
     * 片尾曲
     */
    private String endingTheme;
    /**
     * 原作者
     */
    private String originalAuthor;
    /**
     * 集数
     */
    private String episodeCount;
    /**
     * 原著
     */
    private String originalWork;
    /**
     * 首播电视台
     */
    private String premiereTVStation;
    /**
     * 作品类型(二级分类)
     */
    private List<String> secondCategories;
    private String secondCategory;
    /**
     * 语言
     */
    private String language;
    /**
     * 片头曲
     */
    private String openingTheme;
    /**
     * 播出状态
     */
    private String broadcastStatus;
    /**
     * 别名
     */
    private String aliases;
    /**
     * 年代
     */
    private Integer years;
    /**
     * 本土上映时间
     */

    private LocalDate premiereDate;
    /**
     * 全球上映时间
     */
    private LocalDate worldPremiereDate;
    /**
     * 大陆上映时间
     */
    private LocalDate mainlandPremiereDate;

    public String getProductionRegion() {
        if (productionRegion == null) {
            return null;
        }

        return productionRegion.trim().replaceAll("[、。，\\n.,/]", "|");
    }

    public String getDirectors() {
        if (director == null) {
            return null;
        }
        return director.replaceAll("[、。，\\n.,/]", ",").trim();
    }

    public List<String> getSecondCategories() {
        if (secondCategory == null) {
            return null;
        }
        secondCategory = secondCategory.trim().replaceAll("[、。，\\n.,/]", ",");
        return Stream.of(secondCategory.split(",")).collect(Collectors.toList());
    }

    public String getLanguage() {
        if (language == null) {
            return null;
        }
        return language.trim().replaceAll("[、。，\\n.,/]", "|");
    }

    public String getAliases() {
        String result = null;
        if (aliases == null && englishTitle == null) {
            return null;
        } else if (aliases != null && englishTitle != null) {
            result = aliases + "," + englishTitle;
        } else if (aliases == null) {
            result = englishTitle;
        } else {
            result = aliases;
        }
        return result.trim().replaceAll("[、。，；\\n;.,/等]", ",").trim();
    }


    public void getDate() {
        String releaseInfo = this.releaseDate;

        if (releaseInfo == null) {
            return;
        }
        releaseInfo = releaseInfo.replaceAll("[年月.]", "-").replaceAll("日", "");
        releaseInfo = releaseInfo.replace("/n", ",")
                .replace("\\n", ",")
                .replaceAll("[、，。；;,./ ]", ",");
        String[] split = releaseInfo.split(",");
        this.getDateByRegex(split);
    }

    private void getDateByRegex(String[] split) {
        String regex = "\\d{4}-\\d{1,2}-\\d{1,2}|\\d{4}-\\d{1,2}|\\d{4}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(split[0]);
        if (matcher.find()) {
            String group = matcher.group();
            this.years = Integer.parseInt(group.substring(0, 4));
            this.mainlandPremiereDate = this.parseRegexDate(group);
            this.premiereDate = mainlandPremiereDate;
        }

        // 包含 非中国之外的汉字
        if (split[0].matches(".*[\\u4e00-\\u9fa5].*") && !split[0].matches(".*中国.*")) {
            this.worldPremiereDate = premiereDate;
        }

        if (split.length > 1) {
            for (String s : split) {
                Matcher matchers = pattern.matcher(s);
                if (matchers.find() && s.contains("中国")) {
                    this.mainlandPremiereDate = this.parseRegexDate(matchers.group());
                    break;
                }
            }
        }
    }

    private LocalDate parseRegexDate(String dateStr) {
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("yyyy-MM-d")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        return null; // 所有格式都尝试失败
    }

    public Integer getEpisodeCount() {
        if (episodeCount == null) {
            return null;
        }
        return extractNumber(episodeCount);
    }

    public Integer getDuration() {
        if (duration == null) {
            return null;
        }
        Double v = extractDoubleNumber(duration);
        if (v == null) {
            return null;
        }
        return (int) Math.round(v * 60);
    }

    public Integer extractNumber(String input) {
        if (input == null) {
            return null;
        }
        input = input.trim().replaceAll("[、。，； \\n;,/]", ",");
        String[] split = input.split(",");
        int min = Integer.MAX_VALUE;
        for (String s : split) {
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                min = Math.min(Integer.parseInt(matcher.group()), min);
            }
        }
        if (min == Integer.MAX_VALUE) {
            return null;
        }
        return min;
    }

    public Double extractDoubleNumber(String input) {
        if (input == null) {
            return null;
        }
        input = input.trim().replaceAll("[、。，； \\n;,/]", ",");
        String[] split = input.split(",");
        double min = Double.MAX_VALUE;
        for (String s : split) {
            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                min = Math.min(Double.parseDouble(matcher.group()), min);
            }
        }
        if (min == Double.MAX_VALUE) {
            return null;
        }
        return min;
    }

}
