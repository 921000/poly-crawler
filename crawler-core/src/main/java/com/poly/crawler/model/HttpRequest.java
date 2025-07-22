package com.poly.crawler.model;

import com.poly.crawler.enums.RequestFormat;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Http 请求 通用处理类
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HttpRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private HttpMethod method = HttpMethod.GET;
    private HttpHeaders headers;

    private Map<String, Object> params;

    @Builder.Default
    private RequestFormat format = RequestFormat.JSON;


}

