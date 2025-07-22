package com.poly.crawler.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class SourceRank implements Serializable {


    /**
     * 名称
     */
    private String name;

    /**
     * 排名
     */
    private Integer rankNum;


    /**
     * 连接
     */
    private String url;

}
