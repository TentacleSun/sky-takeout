package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@Slf4j
@Api(tags = "店铺相关用户接口")
@RequestMapping("/user/shop")
public class ShopController {
    static final private String KEY = "SHOP_STATUS";
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> setStatus(){

        Integer status = (Integer)redisTemplate.opsForValue().get(KEY);
        log.info("查询营业状态{}",status);
        return Result.success(status);
    }
}
