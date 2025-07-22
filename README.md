## **`PolyCrawler `项目介绍**


### **1、项目概述：**

- 该项目是一个基于 Java 的通用爬虫系统，专注于从不同网站抓取数据，核心设计理念是提供灵活、可扩展的爬虫框架，同时保证系统的稳定性和高效性。
- 项目不包含 Job 调度相关内容，需通过外部触发，可兼容所有 Job 调度框架（如 Quartz、XXL-Job 等）。
- 核心目标：简化爬虫开发流程，将其拆解为下载与数据处理两大核心阶段。通过封装下载阶段的技术细节（如请求管理、动态渲染、资源控制等），让开发者聚焦于数据处理；同时，在数据处理阶段引入 AI 编码工具或大模型能力，简化非结构化数据到结构化数据的解析流程，最终实现爬虫的高效开发与快速迭代。



### **2、项目结构：**

项目采用模块化设计，主要分为两个核心子模块：

| 模块名称          | 功能描述                                                     |
| ----------------- | ------------------------------------------------------------ |
| `crawler-core`    | 包含爬虫核心功能，如静态资源爬取处理器、无头浏览器爬取处理器、爬虫配置、重试机制、统一调度等。 |
| `crawler-example` | 提供具体的爬虫实现示例（如百度百科、腾讯视频排行榜爬虫），展示如何基于 `crawler-core` 开发实际爬虫任务。 |



### **3、关键功能**

#### 3.1 资源管理与并发控制

###### Playwright 资源池管理

- **资源控制**：通过 `PlaywrightManager` 类管理浏览器实例，基于配置的 `maxCount` 控制最大浏览器实例和页面数量，避免资源耗尽。

- **并发控制**：使用 `Semaphore` 信号量限制并发页面创建数量，确保系统资源稳定。

- **资源复用**：维护页面对象队列，复用已有页面对象，减少重复创建销毁的开销。

- 自动销毁：通过destroy()方法在系统关闭时自动关闭浏览器实例和 Playwright 资源，避免内存泄漏。

  ```java
  // PlaywrightManager 核心资源管理逻辑
  @Override
  public void destroy() {
      log.info("Closing browser.");
      if (browserContexts != null && !browserContexts.isEmpty()) {
          browserContexts.forEach(browserContext -> {
              browserContext.close();
              Browser browser = browserMap.remove(browserContext);
              if (browser != null) browser.close();
              Playwright playwright = playwrightMap.remove(browserContext);
              if (playwright != null) playwright.close();
          });
      }
  }
  ```

###### HTTP 请求管理

- 基于 `RestTemplate` 处理 HTTP 请求，结合 Apache HttpClient 实现可配置的 HTTP 连接池。

- 支持代理配置，灵活应对目标网站的反爬策略。

- 提供便捷的请求方法（如postTextPlain），简化请求构造流程。

  ```java
  // HttpManager 中的 POST 请求示例
  public <T> T postTextPlain(String url, String text, Class<T> responseType) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.TEXT_PLAIN);
      HttpEntity<String> entity = new HttpEntity<>(text, headers);
      ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
      return response.getBody();
  }
  ```



###### 统一线程池调度

- 通过配置文件设定线程池参数（核心线程数、最大线程数、任务队列容量），高效处理并发任务。
- 示例配置：核心线程数 60、最大线程数 120、队列容量 1000，平衡性能与资源占用。





#### 3.2 爬虫框架设计

###### 泛型模板与抽象类

- 核心接口

  ```
  CrawlerProcessor<I, O, R>
  ```

  采用泛型设计，支持灵活定义输入（I）、页面输出（O）、最终结果（R）类型，适配不同爬虫场景：

  - 输入参数类型（I）：可为 URL、请求体等任意类型
  - 页面输出类型（O）：可为 HTTP 响应、HTML 文档、JSON 数据等
  - 最终结果类型（R）：可为处理后的列表、对象等数据结构

- 提供抽象实现类（如`DefaultAbsPlayWrightProcessor`、`DefaultAbsJsoupProcessor`），封装通用逻辑（如下载、重试、资源回收），减少重复开发。

  ```java
  // 泛型爬虫处理器接口定义
  public abstract class DefaultAbsPlayWrightProcessor<I, R> extends AbsCrawlerProcessor<I, Page, R> {
      @Override
      public Page download(CrawlerContext<I, Page> context) {
          // 页面下载通用逻辑（获取页面、导航、等待加载）
      }
      
      @Override
      public abstract R process(CrawlerContext<I, Page> context); // 留给子类实现具体解析逻辑
  }
  ```



###### 完整爬虫任务流程

1. **参数构造**：通过 `addCrawlerContext()` 初始化单个任务上下文，`addAllCrawlerContext()` 支持批量任务初始化。
2. **页面下载**：`download()` 方法负责获取页面内容（支持 Playwright 动态页面或 Jsoup 静态页面）。
3. **结果处理**：`process()` 方法解析页面内容，提取目标数据。
4. **钩子方法**：`before()` 和 `after()` 支持任务执行前后的日志记录、资源清理等操作（如 `after()` 中回收 Playwright 页面）。

###### 重试机制

- 内置重试逻辑，可通过

  ```
  retryExceptions()
  ```

  定义需要重试的异常类型（如`TimeoutError`、`CrawlerRetryException`）。

  ```java
  // 重试异常定义示例
  @Override
  public List<Class<? extends Exception>> retryExceptions() {
      return Stream.of(TimeoutError.class, CrawlerRetryException.class).collect(Collectors.toList());
  }
  ```

- 支持配置最大重试次数和重试间隔，应对网络波动等临时性错误。

#### 3.3 调度与扩展能力

###### 统一调度入口

- 通过`CrawlerService`类管理所有爬虫任务，提供`fetch`（单个任务）、`fetchBatch`（批量任务）等方法，简化调用流程。

  ```java
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
  ```

- 支持多种调度方式：顺序调度、批量调度、总分调度（先访问一个 URL，再处理批量衍生 URL）。



###### 爬虫实现类统一管理

- 通过 `CrawlerEnum` 集中管理爬虫配置（编码、名称、URL、实现类、排序），方便切换和维护。
- 支持扩展为数据库存储，实现动态管理爬虫任务。

###### 工具类支持

- 页面操作工具：`PageUtils`提供页面滚动（scrollPageToBottom）、弹窗处理（handleDialogs）等功能，适配动态页面交互需求。

  ```java
  // 滚动页面到底部（间隔 300 毫秒）
  public static void scrollPageToBottom(Page page) {
      page.evaluate("() => {\n" +
              "    return new Promise((resolve, reject) => {\n" +
              "        const totalHeight = document.documentElement.scrollHeight;\n" +
              "        let distance = 500;\n" +
              "        let lastScrollY = 0;\n" +
              "        let noChangeCount = 0;\n" +
              "        const maxNoChangeCount = 5; // 设置最大连续无变化次数\n" +
              "        let timer = setInterval(() => {\n" +
              "            window.scrollBy(0, distance);\n" +
              "            let currentScrollY = window.scrollY;\n" +
              "            if (currentScrollY === lastScrollY) {\n" +
              "                noChangeCount++;\n" +
              "                if (noChangeCount >= maxNoChangeCount) {\n" +
              "                    clearInterval(timer);\n" +
              "                    resolve();\n" +
              "                }\n" +
              "            } else {\n" +
              "                noChangeCount = 0;\n" +
              "            }\n" +
              "            lastScrollY = currentScrollY;\n" +
              "        }, 300);\n" +
              "    });\n" +
              "}");
  }
  ```

- **通用工具**：`CommonUtils` 提供随机休眠（`randomSleep`）等功能，模拟人工操作间隔，降低反爬风险。

###### 配置管理

- 通过CrawlerProperties集中管理框架配置，支持代理、HTTP 连接池、线程池、重试策略等参数的灵活配置：

  ```java
  @Component
  @ConfigurationProperties(prefix = "crawler")
  @Data
  public class CrawlerProperties {
      private Proxy proxy; // 代理配置
      private Http http;   // HTTP连接池配置
      private Thread thread; // 线程池配置
      private int maxRetries; // 最大重试次数
      private long retryDelayMs; // 重试间隔(毫秒)
      private int batchTimeoutSeconds; // 批量超时时间(秒)
      private int timeoutSeconds; // 单个请求超时时间(秒)
  }
  ```

#### 3.4 示例爬虫说明

###### 百度百科爬虫（`BaiduBaiKeProcessor`）

- 功能：抓取百度百科页面的基本信息（如概述、ID、剧情介绍等）。

- 关键实现：通过 XPath 选择器提取页面元素，处理文本清洗（如去除引用标记）。

  ```java
  @Override
  public Map<String,String> process(CrawlerContext<String, Page> context) {
      Page page = context.getOutput();
      List<ElementHandle> itemNameElements = page.querySelectorAll("xpath=//dt[contains(@class, 'itemName_')]");
      List<ElementHandle> itemValueElements = page.querySelectorAll("xpath=//dd[contains(@class, 'itemValue_')]");
      Map<String, String> itemMap = new HashMap<>();
      for (int i = 0; i < itemNameElements.size(); i++) {
          String itemName = itemNameElements.get(i).innerText();
          String itemValue = itemValueElements.get(i).innerText().replaceAll("\\[\\d+]", "").trim();
          itemMap.put(itemName, itemValue);
      }
      // 提取ID和概要信息
      ElementHandle element = page.querySelector("#J-vars");
      itemMap.put("summary", this.getSummary(page));
      itemMap.put("id", element.getAttribute("data-lemmaid"));
      return itemMap;
  }
  ```

###### 腾讯视频排行榜爬虫（`TencentChildrenRankProcessor`）

- 功能：抓取腾讯视频儿童频道排行榜数据（排名、名称、URL）。
- 关键实现：通过 CSS 选择器定位列表项，提取排名和内容信息。



### **4、代码优势分析**

1. **模块化与低耦合**
   核心功能与示例分离，`crawler-core` 提供通用能力，`crawler-example` 展示具体实现，便于维护和扩展。
2. **资源高效利用**
   Playwright 资源池通过复用页面对象和信号量控制，减少浏览器实例创建销毁的开销，降低系统资源占用。
3. **灵活性与通用性**
   泛型设计适配不同数据类型，支持动态页面（**Playwright**）和静态页面（**Jsoup**），可应对多种网站结构。
4. **稳定性保障**
   重试机制、线程池控制、资源自动回收等设计，确保系统在高并发或网络不稳定时的可靠性。
5. **易扩展性**
   统一接口和抽象类简化新爬虫的开发，扩展点支持未来通过配置或脚本扩展功能，无需修改核心代码。



### **5、快速上手指南**

#### 环境依赖

- JDK 版本：8（通过项目配置 `languageLevel="JDK_1.8"` 确认）
- 依赖管理：Maven（项目包含 pom.xml 配置）
- Playwright 浏览器：需提前安装（框架自动管理实例，支持无头模式）,配置文件指定谷歌浏览器路径也可

#### 开发步骤

1. **继承抽象类**：根据页面类型选择继承 `DefaultAbsPlayWrightProcessor`（动态页面）或 `DefaultAbsJsoupProcessor`（静态页面）。
2. 实现核心方法：
   - `process()`：定义页面解析逻辑，提取目标数据。
   - `addCrawlerContext()`：初始化爬虫任务参数（如 URL）。
3. **配置调度**：通过 `CrawlerService` 调用 `fetch` 或 `fetchBatch` 方法执行任务。



### **6、后续计划**

1. 页面解析逻辑通过ai 将页面非结构化解析为结构化数据
2. 完善测试用例与部署方案。
