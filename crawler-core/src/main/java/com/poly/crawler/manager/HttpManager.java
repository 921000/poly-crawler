package com.poly.crawler.manager;


import com.alibaba.fastjson.JSONObject;
import com.poly.crawler.enums.RequestFormat;
import com.poly.crawler.util.MapWrapper;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * HttpManager http 处理工具类
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@Component("crawlerHttpManager")
public class HttpManager {

    @Resource(name = "crawlerRestTemplate")
    private RestTemplate restTemplate;

    /**
     * 发送GET请求
     *
     * @param url          请求URL
     * @param responseType 响应类型
     * @param <T>          响应类型泛型
     * @return 响应对象
     */
    public <T> T get(String url, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
        response.getHeaders().forEach((key, values) -> {
            values.forEach(value -> {
                System.out.println(key + ":" + value);
            });
        });
        return response.getBody();
    }

    /**
     * 发送GET请求，带请求头
     *
     * @param url          请求URL
     * @param headers      请求头
     * @param responseType 响应类型
     * @param <T>          响应类型泛型
     * @return 响应对象
     */
    public <T> T get(String url, HttpHeaders headers, Class<T> responseType) {
        if (headers == null) {
            return get(url, responseType);
        }
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        return response.getBody();
    }

    /**
     * 发送POST请求，JSON格式
     *
     * @param url          请求URL
     * @param request      请求体
     * @param responseType 响应类型
     * @param <T>          响应类型泛型
     * @param <R>          请求体类型泛型
     * @return 响应对象
     */
    public <T, R> T postJson(String url, R request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<R> entity = new HttpEntity<>(request, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }

    /**
     * 发送POST请求，XML格式
     *
     * @param url          请求URL
     * @param request      请求体
     * @param responseType 响应类型
     * @param <T>          响应类型泛型
     * @param <R>          请求体类型泛型
     * @return 响应对象
     */
    public <T, R> T postXml(String url, R request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<R> entity = new HttpEntity<>(request, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }

    /**
     * 发送POST请求，FORM_DATA格式
     *
     * @param url          请求URL
     * @param formData     表单数据
     * @param responseType 响应类型
     * @param <T>          响应类型泛型
     * @return 响应对象
     */
    public <T> T postFormData(String url, MultiValueMap<String, String> formData, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }

    /**
     * 发送POST请求，TEXT_PLAIN格式
     *
     * @param url          请求URL
     * @param text         文本内容
     * @param responseType 响应类型
     * @param <T>          响应类型泛型
     * @return 响应对象
     */
    public <T> T postTextPlain(String url, String text, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>(text, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }

    /**
     * 发送POST请求，MULTIPART_FORM_DATA格式
     *
     * @param url           请求URL
     * @param multipartData 多部分数据
     * @param responseType  响应类型
     * @param <T>           响应类型泛型
     * @return 响应对象
     */
    public <T> T postMultipartFormData(String url, MultiValueMap<String, Object> multipartData, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(multipartData, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }

    /**
     * 创建一个包含文件的MultiValueMap
     *
     * @param file 文件
     * @return MultiValueMap
     */
    public MultiValueMap<String, Object> createMultipartData(File file) {
        MultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
        multipartData.add("file", new FileSystemResource(file));
        return multipartData;
    }


    public <T> T download(String url, RequestFormat format, Map<String, Object> request, Class<T> responseType) {
        switch (format) {
            case JSON:
                return postJson(url, convertToJsonObject(request), responseType);
            case XML:
                return postXml(url, convertToXmlString(request), responseType);
            case FORM_DATA:
                return postFormData(url, convertToFormData(request), responseType);
            case TEXT_PLAIN:
                return postTextPlain(url, convertToPlainText(request), responseType);
            case MULTIPART_FORM_DATA:
                return postMultipartFormData(url, convertToMultipartFormData(request), responseType);
            default:
                throw new IllegalArgumentException("Unsupported RequestFormat: " + format);
        }
    }

    private JSONObject convertToJsonObject(Map<String, Object> request) {
        // 将 Map 转换为 JSONObject
        return new JSONObject(request);
    }

    private String convertToXmlString(Map<String, Object> request) {
        try {
            JAXBContext context = JAXBContext.newInstance(MapWrapper.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            MapWrapper mapWrapper = new MapWrapper();
            mapWrapper.setEntries(new ArrayList<>(request.entrySet()));

            java.io.StringWriter sw = new java.io.StringWriter();
            marshaller.marshal(mapWrapper, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to convert Map to XML", e);
        }
    }

    private MultiValueMap<String, String> convertToFormData(Map<String, Object> request) {
        // 将 Map 转换为 MultiValueMap<String, String>
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            formData.add(entry.getKey(), entry.getValue().toString());
        }
        return formData;
    }

    private String convertToPlainText(Map<String, Object> request) {
        // 将 Map 转换为纯文本字符串
        // 这里可以根据具体需求进行实现
        return request.toString();
    }

    private MultiValueMap<String, Object> convertToMultipartFormData(Map<String, Object> request) {
        // 将 Map 转换为 MultiValueMap<String, Object>
        MultiValueMap<String, Object> multipartFormData = new LinkedMultiValueMap<>();
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            multipartFormData.add(entry.getKey(), entry.getValue());
        }
        return multipartFormData;
    }
}

