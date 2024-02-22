package com.sky.result;

import com.sky.entity.LocationQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationQueryResult {
    //百度地图返回状态
    private Integer status;

    //百度地图返回内容
    private LocationQuery result;

}
