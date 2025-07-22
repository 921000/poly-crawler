package com.poly.crawler.process;

import com.poly.crawler.exception.CrawlerException;
import com.poly.crawler.manager.HttpManager;
import com.poly.crawler.model.CrawlerContext;
import com.poly.crawler.model.HttpRequest;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Resource;
import lombok.Getter;
import org.springframework.http.HttpMethod;

/**
 * DefaultHttpProcessor http 请求默认处理器
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Getter
public abstract class DefaultAbsHttpProcessor<I extends HttpRequest, O, R> extends AbsCrawlerProcessor<I, O, R> {

    private final Class<I> inputType;
    private final Class<O> outputType;
    private final Class<R> responseType;

    @Resource(name = "crawlerHttpManager")
    private HttpManager httpManager;

    @SuppressWarnings("unchecked")
    protected DefaultAbsHttpProcessor() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            this.inputType = (Class<I>) actualTypeArguments[0];
            this.outputType = (Class<O>) actualTypeArguments[1];
            this.responseType = (Class<R>) actualTypeArguments[2];
        } else {
            throw new IllegalArgumentException("Invalid type specification");
        }
    }

    @Override
    public O download(CrawlerContext<I, O> context) {
        I input = context.getParams();
        if (input == null) {
            throw new CrawlerException("Params cannot be null");
        }
        if (HttpMethod.GET.equals(input.getMethod())) {
            return httpManager.get(context.getUrl(), input.getHeaders(), this.getOutputType());
        }
        return httpManager.download(context.getUrl(), input.getFormat(), input.getParams(), this.getOutputType());
    }

    @Override
    public abstract R process(CrawlerContext<I, O> context);


}
