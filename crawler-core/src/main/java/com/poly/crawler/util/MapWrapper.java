package com.poly.crawler.util;

import java.util.List;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;


/**
 * MapWrapper 类描述
 *
 * @author guojund
 * @version 2024/12/21
 * @since 2024-12-21
 */
@XmlRootElement(name = "map")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class MapWrapper {

    @XmlAnyElement
    private List<Entry<String, Object>> entries;
}
