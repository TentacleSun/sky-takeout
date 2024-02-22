package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * 百度地图请求的返回结果
 * */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationQuery {
    private Location location;
    private Integer precise;
    private Integer confidence;
    private Integer comprehension;

    private String level;
    private String analys_level;
}
