package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.shop")
@Data
public class ShopLocationProperties {
    //商店地址
    private String address;
    //认证信息
    private String baiduak;
    private String baidusk;
}
